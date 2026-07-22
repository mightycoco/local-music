# Local Music

Local Music is an Android music player focused exclusively on local audio playback. The project is designed for source-first development in Visual Studio Code using Kotlin, Gradle, Jetpack Compose, AndroidX Media3, Room, Coroutines, Flow, MVVM, and Clean Architecture.

## Current Increment

This repository currently contains buildable architecture and feature foundations:

- Gradle Kotlin DSL Android app module.
- Compose Material 3 application shell.
- Clean Architecture package layout.
- Domain models for songs, filters, and sort orders.
- Repository boundary that keeps UI isolated from MediaStore.
- Room schema foundation for local song metadata.
- Initial home screen with visible search, filters, and sorting controls.
- MediaStore library scanning with Room-backed persistence.
- Storage Access Framework folder-source scanning with persisted tree URIs.
- Runtime audio permission request for Android 13+ and legacy storage access.
- Duplicate hiding across MediaStore and user-selected folders.
- Instant metadata search, filters, sorting, and adaptive library layout.
- Persistent favourites with song-row toggle controls.
- Media3 playback service, queue mapping, and song-tap playback.
- M3U playlist import/export codec and SAF picker workflows.
- Embedded album-art extraction, disk cache pruning, and song-list thumbnails.
- Bluetooth car detection, runtime Bluetooth permission handling, settings import/export, and license foundations.
- Bottom navigation with Home, Now Playing, Playlists, Favourites, and Settings destinations.
- Now Playing controls wired through the Media3 playback boundary.
- Follow System, Dark, and Light theme modes.
- Unit test for the first use case.

## Build From VS Code

Install the Android SDK and JDK 17 before building. In `cmd.exe`, make sure Java is available to Gradle:

```cmd
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
set "ANDROID_HOME=%AppData%\..\Local\Android\SDK"
set "ANDROID_SDK=%AppData%\..\Local\Android\SDK"
set "PATH=%JAVA_HOME%\bin;%PATH%"
```

Run tests:

```cmd
gradlew.bat test
```

Create a debug APK:

```cmd
gradlew.bat assembleDebug
```

Output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Create a release APK:

```cmd
gradlew.bat assembleRelease
```

Output:

```text
app\build\outputs\apk\release\app-release-unsigned.apk
```

Create the Google Play Store upload bundle:

```cmd
gradlew.bat bundleRelease
```

Output:

```text
app\build\outputs\bundle\release\app-release.aab
```

Release APKs and Play Store bundles must be signed before distribution. Configure release signing in Gradle before publishing outside a local debug workflow.

## Development Notes

- The app plays local files only; no streaming or cloud integrations are planned.
- MediaStore and Storage Access Framework scanning should be implemented behind `MusicRepository`.
- UI classes must not access MediaStore directly.
- Keep feature increments small, documented, tested where practical, and buildable.
