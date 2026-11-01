# Repository format

Tachiyomi-compatible extension repositories commonly publish an index at `index.min.json`. The exact schema and compatibility requirements vary by app release and repository generation.

This project currently publishes an empty JSON array (`[]`) as a safe, valid JSON placeholder. Do not add guessed metadata fields: verify the format against the target Tachiyomi build and a known-good repository compatible with that exact build before publishing extensions.

For each future extension, verify the expected index object fields, APK naming convention, download URL, package identifier, version code, version name, language, and NSFW flag against the target app's parser. The index entry must match the APK byte-for-byte release that it describes.

The raw index URL is:

`https://raw.githubusercontent.com/HurairaBaig/tachiyomi-extensions/main/index.min.json`

An empty index means no extensions are offered yet.