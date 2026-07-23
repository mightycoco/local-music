# Changelog

## 0.1.0 - Unreleased

- Created the initial Android Kotlin project scaffold.
- Added Gradle Kotlin DSL configuration for Compose, Media3, Room, Coroutines, and tests.
- Added Clean Architecture domain models, repository boundary, and first use case.
- Added initial Compose home screen with always-visible search, filters, and sort controls.
- Added Room song metadata schema foundation.
- Added project architecture, backlog, and build documentation.
- Added MediaStore scanning behind the repository boundary.
- Added runtime audio permission flow for indexing local songs.
- Added Room-backed library refresh with duplicate hiding.
- Added duplicate sorting and de-duplication tests.
- Added Storage Access Framework folder source persistence and recursive folder scanning.
- Added composite MediaStore plus SAF scanning with cross-source duplicate hiding.
- Added instant metadata search, filtering, sorting, and a favourites panel foundation.
- Added Media3 playback service registration and queue item mapping.
- Added M3U playlist import/export codec with tests.
- Added artwork lookup, Bluetooth car-device detection, adaptive layout, settings backup, and license foundations.
- Added persistent favourite toggles from song rows.
- Connected song taps to the Media3 playback service.
- Added SAF playlist import/export picker workflows.
- Added embedded album-art extraction, disk cache pruning, and song-list thumbnails.
- Added Bluetooth runtime permission handling and automatic Car Mode state detection.
- Added bottom navigation for Home, Now Playing, Playlists, Favourites, and Settings.
- Added Now Playing controls backed by Media3 playback commands for play, pause, next, previous, and seek.
- Added Follow System, Dark, and Light theme modes with Follow System as the default.
- Limited automatic library enumeration to the initial ViewModel refresh to keep navigation responsive.
- Synced Now Playing progress and selected song state from Media3 playback updates, including external media controls.
- Updated bottom navigation to use larger icon-only destinations.
- Scoped library search, filters, and import/export actions to the Home screen.
- Persisted imported playlists locally and added delete actions on the Playlists screen.
- Fixed favourite retention during library refresh and made playlist writes durable before returning.
- Added a playlist creation dialog and Now Playing actions to add the current song to new or existing playlists.
- Added a protected Queue playlist that is always listed first, can be cleared, and accepts songs from Now Playing and long-press library actions.
- Added long-press media actions for queueing or adding a song to a new or existing playlist.
- Added locally embedded artwork as a subtle backdrop on Now Playing while preserving the local-files-only playback boundary.
