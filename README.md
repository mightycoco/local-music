# Local Music

Local Music is an Android music player focused exclusively on local audio playback. The project is designed for source-first development in Visual Studio Code using Kotlin, Gradle, Jetpack Compose, AndroidX Media3, Room, Coroutines, Flow, MVVM, and Clean Architecture.

## Current Increment

This repository currently contains a buildable local-playback foundation and selected end-user features. The remaining product requirements are tracked explicitly in `TODO.md`.

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
- Persistent M3U playlist import/export, basic creation/deletion, and Queue persistence with Media3 enqueue support.
- Embedded album-art extraction, disk cache pruning, and song-list thumbnails.
- Bluetooth car-audio detection, runtime Bluetooth permission handling, settings import/export, and license foundations.
- Bottom navigation with Home, Now Playing, Playlists, Favourites, and Settings destinations.
- Now Playing play, pause, previous, next, seek, playlist, and Queue controls wired through the Media3 playback boundary.
- Follow System, Dark, and Light theme modes.
- Unit, repository, playlist persistence, artwork-cache, and settings serialization tests.

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

## Release Signing and CI

Debug APKs are signed with Android's debug key and can be installed for development. Release artifacts are signed only when the following environment variables are available: `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`.

Generate and retain a private release/upload key outside the repository:

```cmd
keytool -genkeypair -keystore local-music-release.p12 -storetype PKCS12 -alias local-music -keyalg RSA -keysize 4096 -validity 10000
```

For GitHub Actions, add these repository secrets under **Settings > Secrets and variables > Actions**:

- `ANDROID_KEYSTORE_BASE64`: the base64 encoding of `local-music-release.p12`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

The CI workflow uploads artifacts for every non-tag run: a debug APK plus unsigned release APK and AAB files. These temporary artifacts are retained for 24 hours. Tags beginning with `v` run the signed release job instead. After all four secrets are configured, it uploads an installable signed release APK and AAB without specifying an artifact retention period, so GitHub applies the repository default. Keep the keystore and passwords private and backed up; they are required for app updates. Use this key as the upload key when enabling Google Play App Signing.

## Development Notes

- The app plays local files only; no streaming or cloud integrations are planned.
- MediaStore and Storage Access Framework scanning should be implemented behind `MusicRepository`.
- UI classes must not access MediaStore directly.
- Keep feature increments small, documented, tested where practical, and buildable.
