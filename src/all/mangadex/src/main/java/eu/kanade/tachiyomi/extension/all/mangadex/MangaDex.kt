package eu.kanade.tachiyomi.extension.all.mangadex

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Original Tachiyomi 0.x source. Only MangaDex Safe-rated manga are listed. */
class MangaDex : HttpSource() {
    override val name = "MangaDex Safe"
    override val baseUrl = "https://mangadex.org"
    override val lang = "all"
    override val supportsLatest = false

    private val apiUrl = "https://api.mangadex.org"
    private val pageSize = 50
    private val chapterPageSize = 500

    override fun latestUpdatesRequest(page: Int): Request {
        val url = api("$apiUrl/manga") {
            addQueryParameter("limit", pageSize.toString())
            addQueryParameter("offset", ((page - 1) * pageSize).toString())
            addQueryParameter("order[updatedAt]", "desc")
            addQueryParameter("contentRating[]", "safe")
            addQueryParameter("includes[]", "cover_art")
        }
        return GET(url)
    }

    override fun popularMangaRequest(page: Int): Request {
        val url = api("$apiUrl/manga") {
            addQueryParameter("limit", pageSize.toString())
            addQueryParameter("offset", ((page - 1) * pageSize).toString())
            addQueryParameter("order[followedCount]", "desc")
            addQueryParameter("contentRating[]", "safe")
            addQueryParameter("includes[]", "cover_art")
        }
        return GET(url)
    }

    override fun popularMangaParse(response: Response): MangasPage = parseMangaPage(response)

    override fun latestUpdatesParse(response: Response): MangasPage = MangasPage(emptyList(), false)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = api("$apiUrl/manga") {
            addQueryParameter("limit", pageSize.toString())
            addQueryParameter("offset", ((page - 1) * pageSize).toString())
            addQueryParameter("title", query)
            addQueryParameter("order[relevance]", "desc")
            addQueryParameter("contentRating[]", "safe")
            addQueryParameter("includes[]", "cover_art")
        }
        return GET(url)
    }

    override fun searchMangaParse(response: Response): MangasPage = parseMangaPage(response)

    override fun getFilterList() = FilterList()

    override fun mangaDetailsRequest(manga: SManga): Request {
        val url = api("$apiUrl/manga/${manga.url}") {
            addQueryParameter("includes[]", "cover_art")
            addQueryParameter("includes[]", "author")
            addQueryParameter("includes[]", "artist")
        }
        return GET(url)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val root = JSONObject(response.body!!.string()).getJSONObject("data")
        val attributes = root.getJSONObject("attributes")
        require(attributes.optString("contentRating") == "safe") {
            "This source only allows Safe-rated MangaDex titles."
        }

        val manga = SManga.create()
        manga.url = root.getString("id")
        manga.title = localizedValue(attributes.optJSONObject("title"))
        manga.description = localizedValue(attributes.optJSONObject("description"))
        manga.status = when (attributes.optString("status")) {
            "ongoing" -> SManga.ONGOING
            "completed" -> SManga.COMPLETED
            "hiatus" -> SManga.ON_HIATUS
            "cancelled" -> SManga.CANCELLED
            else -> SManga.UNKNOWN
        }

        val tags = attributes.optJSONArray("tags") ?: JSONArray()
        val genres = mutableListOf<String>()
        for (i in 0 until tags.length()) {
            val tag = tags.optJSONObject(i) ?: continue
            val tagAttributes = tag.optJSONObject("attributes") ?: continue
            val tagName = localizedValue(tagAttributes.optJSONObject("name"))
            if (tagName.isNotBlank()) genres += tagName
        }
        manga.genre = genres.distinct().joinToString(", ")

        val relationships = root.optJSONArray("relationships") ?: JSONArray()
        for (i in 0 until relationships.length()) {
            val relation = relationships.optJSONObject(i) ?: continue
            when (relation.optString("type")) {
                "cover_art" -> {
                    val fileName = relation.optJSONObject("attributes")?.optString("fileName").orEmpty()
                    if (fileName.isNotBlank()) {
                        manga.thumbnail_url = "https://uploads.mangadex.org/covers/${manga.url}/$fileName.256.jpg"
                    }
                }
                "author", "artist" -> {
                    val creator = relation.optJSONObject("attributes")?.optString("name").orEmpty()
                    if (creator.isNotBlank()) {
                        manga.author = listOfNotNull(manga.author, creator).distinct().joinToString(", ")
                    }
                }
            }
        }
        manga.initialized = true
        return manga
    }

    override fun chapterListRequest(manga: SManga): Request {
        val url = api("$apiUrl/manga/${manga.url}/feed") {
            addQueryParameter("limit", chapterPageSize.toString())
            addQueryParameter("offset", "0")
            addQueryParameter("order[chapter]", "desc")
            addQueryParameter("includes[]", "scanlation_group")
        }
        return GET(url)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val first = JSONObject(response.body!!.string())
        val total = first.optInt("total", 0)
        val chapters = mutableListOf<SChapter>()
        appendChapters(first.optJSONArray("data"), chapters)

        val requestUrl = response.request.url
        var offset = chapterPageSize
        while (offset < total) {
            val nextUrl = requestUrl.newBuilder().setQueryParameter("offset", offset.toString()).build()
            client.newCall(GET(nextUrl)).execute().use { nextResponse ->
                if (!nextResponse.isSuccessful) throw Exception("MangaDex chapter request failed: ${nextResponse.code}")
                val json = JSONObject(nextResponse.body!!.string())
                appendChapters(json.optJSONArray("data"), chapters)
            }
            offset += chapterPageSize
        }
        return chapters
    }

    override fun pageListRequest(chapter: SChapter): Request =
        GET("$apiUrl/at-home/server/${chapter.url}")

    override fun pageListParse(response: Response): List<Page> {
        val root = JSONObject(response.body!!.string())
        val base = root.getString("baseUrl")
        val chapter = root.getJSONObject("chapter")
        val hash = chapter.getString("hash")
        val data = chapter.getJSONArray("data")
        return (0 until data.length()).map { index ->
            val fileName = data.getString(index)
            Page(index, "", "$base/data/$hash/$fileName")
        }
    }

    override fun imageUrlParse(response: Response): String = response.request.url.toString()

    override fun getMangaUrl(manga: SManga): String = "$baseUrl/title/${manga.url}"
    override fun getChapterUrl(chapter: SChapter): String = "$baseUrl/chapter/${chapter.url}"

    private fun parseMangaPage(response: Response): MangasPage {
        val root = JSONObject(response.body!!.string())
        val data = root.optJSONArray("data") ?: JSONArray()
        val manga = mutableListOf<SManga>()
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val attributes = item.optJSONObject("attributes") ?: continue
            if (attributes.optString("contentRating") != "safe") continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            val entry = SManga.create()
            entry.url = id
            entry.title = localizedValue(attributes.optJSONObject("title"))
            entry.thumbnail_url = coverUrl(item, id)
            if (entry.title.isNotBlank()) manga += entry
        }
        val total = root.optInt("total", 0)
        val offset = root.optInt("offset", 0)
        return MangasPage(manga, offset + data.length() < total)
    }

    private fun appendChapters(data: JSONArray?, destination: MutableList<SChapter>) {
        if (data == null) return
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val attributes = item.optJSONObject("attributes") ?: continue
            val chapter = SChapter.create()
            chapter.url = item.optString("id")
            val number = attributes.optString("chapter")
            val title = attributes.optString("title")
            chapter.chapter_number = number.toFloatOrNull() ?: -1f
            chapter.name = buildString {
                append(if (number.isBlank() || number == "null") "Oneshot" else "Chapter $number")
                if (title.isNotBlank() && title != "null") append(" - $title")
            }
            chapter.date_upload = parseDate(attributes.optString("publishAt"))
            val relationships = item.optJSONArray("relationships") ?: JSONArray()
            for (j in 0 until relationships.length()) {
                val relation = relationships.optJSONObject(j) ?: continue
                if (relation.optString("type") == "scanlation_group") {
                    val group = relation.optJSONObject("attributes")?.optString("name").orEmpty()
                    if (group.isNotBlank()) chapter.scanlator = group
                }
            }
            destination += chapter
        }
    }

    private fun coverUrl(item: JSONObject, mangaId: String): String? {
        val relationships = item.optJSONArray("relationships") ?: return null
        for (i in 0 until relationships.length()) {
            val relation = relationships.optJSONObject(i) ?: continue
            if (relation.optString("type") == "cover_art") {
                val fileName = relation.optJSONObject("attributes")?.optString("fileName").orEmpty()
                if (fileName.isNotBlank()) return "https://uploads.mangadex.org/covers/$mangaId/$fileName.256.jpg"
            }
        }
        return null
    }

    private fun localizedValue(values: JSONObject?): String {
        if (values == null) return ""
        val english = values.optString("en")
        if (english.isNotBlank() && english != "null") return english
        val keys = values.keys()
        while (keys.hasNext()) {
            val value = values.optString(keys.next())
            if (value.isNotBlank() && value != "null") return value
        }
        return ""
    }

    private fun parseDate(value: String): Long =
        runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(0L)

    private fun api(path: String, configure: HttpUrl.Builder.() -> Unit): HttpUrl =
        path.toHttpUrl().newBuilder().apply(configure).build()
}
