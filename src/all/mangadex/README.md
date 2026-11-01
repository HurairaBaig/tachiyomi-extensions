# MangaDex Safe (legacy Tachiyomi)

This module targets the original Tachiyomi extension API, `extensions-lib:1.4`.

- Popular and search requests ask MangaDex for `contentRating[]=safe`.
- Results are checked again; only exact `safe` ratings are retained.
- Manga details reject any title not explicitly rated `safe`.
- No chapter-language restriction is sent, allowing available languages for Safe-rated titles.
- The build workflow uploads a debug APK artifact; it is not published in the extension index until it has been tested in original Tachiyomi v0.15.3.

MangaDex and relevant scanlation groups must be credited. API usage is subject to the current MangaDex policy: https://api.mangadex.org/docs/
