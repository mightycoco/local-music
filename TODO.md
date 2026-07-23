# TODO

## Current Foundation

- [x] Create VS Code friendly Android/Kotlin project skeleton.
- [x] Add Clean Architecture package layout.
- [x] Add initial Compose home screen with visible search, filters, and sorting controls.
- [x] Add Room metadata schema foundation.
- [x] Add initial domain use case test.

## Next Feature Slice

- [x] Add MediaStore scanner using `MediaStore.DATE_ADDED` for newest-added ordering.
- [x] Add runtime audio permission flow for Android 13+ and legacy storage read permission.
- [x] Persist scanned songs into Room.
- [x] Replace `InMemoryMusicRepository` with a Room-backed repository.
- [x] Add repository tests for sorting and duplicate hiding.

## Backlog

- [x] Storage Access Framework folder sources.
- [x] Duplicate detection across MediaStore and user folders.
- [x] Instant metadata search.
- [x] Media3 playback service and queue.
- [x] Playlists with M3U import/export.
- [x] Favourites screen foundation.
- [x] Artwork disk cache.
- [x] Artwork lookup pipeline foundation.
- [x] Bluetooth car device detection foundation.
- [x] Adaptive layouts for tablets and foldables.
- [x] Settings import/export and licenses foundation.

## Follow-up Backlog

- [x] Persist and toggle favourites from the song list.
- [x] Connect song taps to the Media3 playback service.
- [x] Add full playlist management screens and SAF import/export pickers.
- [x] Add album-art extraction, disk cache pruning, and UI image loading.
- [x] Add runtime Bluetooth permissions and automatic Car Mode UI switching.
- [x] Starting a playback opens a detailed view of the song with song controls (next/previous/progress slider control/play/pause)
- [x] The selection of the screen must be at the bottom (Home/Now Playing/Playlists/Favourites/Settings)
- [x] The default setting for the theme must be "Follow System", other options are Dark, Light
- [x] The navigation is very slow. The enumeration of media files seems to be done on any UI input
- [x] Keep Now Playing progress and selected song synchronized with Media3 playback state and external media controls.
- [x] Favourite songs must be persisted and retained across library refreshes/app restarts.
- [x] Playlist screen must allow deleting playlists
- [x] Now Playing must allow adding to a new/existing playlist
- [x] Queueing of items must be possible. The Queue must behave like a playlist, not be deletable, can be cleared, must be always at the top of the list of playlists
- [x] Playlists must be persisted and loaded after app restart.
- [x] Media list items must have a long press menu with the options to queue, add to a new/existing playlist
- [x] Creating a playlist should just show a modal dialog with a generic TextInput field and Cancel/Create buttons
- [x] The bottom nav buttons must have a larger icon, no text (home, now playing, playlists, favourites, settings)
- [x] The media list filters and search bar should only be visible on the home screen, not on the other screens.
- [x] The now playing screen uses locally available song artwork as a backdrop. Online artwork lookup is intentionally excluded because this is a local-files-only player.