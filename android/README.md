# Hayasaka Android

Native Android music app scaffold for package `com.android.wx2.android`.

## Features (v1.0.1 work in progress)
- Kotlin + Jetpack Compose, Material 3 UI
- Download direct HTTPS audio-file URLs using Android DownloadManager
- Offline library in app-private storage, listing and playback of downloaded tracks
- Delete downloaded tracks with an explicit confirmation dialog
- Media3 ExoPlayer playback service and MediaSession for system playback integration

## Build
Open the `android/` folder in Android Studio and run `assembleDebug`, or download the debug APK from the Actions run's artifacts once CI succeeds.

## Limitations
This remains an early native app: no remote catalog search or streaming, playlists, lyrics, metadata editing, queue/shuffle/repeat UI, equalizer, sleep timer, or app-level download progress/retry yet. Direct-download sources must provide audio files and permit downloads; the app does not bypass DRM or source restrictions.

## Feature research / roadmap
Prioritized from current Android music-player patterns:
1. Playback queue, previous/next, seekbar, shuffle and repeat.
2. Media notification/lockscreen controls with robust service lifecycle.
3. Browse device music using MediaStore (instead of only app-managed downloads).
4. Track metadata and album artwork, richer library views.
5. Playlists and favorites, persistent storage (Room).
6. Lyrics, sleep timer and equalizer.
7. Download progress, retry/cancel, collision-safe filenames.
8. Backup/export and settings, then tests and signed release distribution.

## Release notes
See [CHANGELOG.md](CHANGELOG.md). Version 1.0.1 is being prepared; CI must pass before a release is tagged.
