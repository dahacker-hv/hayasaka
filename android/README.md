# Hayasaka Android (starter)

Native Android app scaffold for package `com.android.wx2.android`.

## Included
- Kotlin + Jetpack Compose starter UI
- HTTPS/HTTP URL entry for audio downloads
- Android DownloadManager downloads into Music/Hayasaka
- System download notifications

## Build
Open the `android/` folder in Android Studio, let Gradle sync, then run on an Android device.

## Current limitations
This is a starter build, not the finished player. It does not yet implement in-app offline library browsing/playback, Media3 background playback, MediaSession lockscreen/Bluetooth controls, playlists, or metadata tagging. Download sources must permit direct audio-file downloads; this does not bypass DRM or source restrictions.