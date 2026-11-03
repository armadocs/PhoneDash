# PhoneDash

A full-page Android home-screen widget showing the Armada daily brief (Notion), clock, date and weather.
Same data as FrameDash on the Lenovo frame. Tap an item to tick it off; it syncs to Notion.

- Every push to `main` builds a signed APK and publishes a GitHub release (needs secret `PHONEDASH_KEYSTORE_PASSWORD`, ASCII only).
- The Notion key is pasted into the app on the phone; it is never stored in this repo.
- Updates: open PhoneDash → Check for updates. The widget also shows when a new version is out.
