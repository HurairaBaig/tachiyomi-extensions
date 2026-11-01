# HurairaBaig Tachiyomi Extension Repository

A community repository endpoint for the original Tachiyomi Android app.

## Repository URL

Add this URL in Tachiyomi under **More → Settings → Browse → Extension repos**:

```
https://raw.githubusercontent.com/HurairaBaig/tachiyomi-extensions/main/index.min.json
```

## Current status

This repository is initialized, but it does **not yet contain any installable extensions**. The index is intentionally empty rather than listing unverified or unsafe APKs.

An extension repository is a catalogue of Android APKs plus metadata. Adding an empty index successfully tests that the app can reach this repository, but it will not add manga sources.

## Important safety note

Tachiyomi extensions are Android applications and can access data available to them. Only install extensions from maintainers you trust. This repository will only list extensions whose source code, licensing, compatibility, and download artifacts can be verified. It will not host pirated manga or reproduce removed proprietary extensions.

## Next step

Extensions must be developed and built separately for the exact Tachiyomi API/version, then their APK metadata and download files can be added to this repository.