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
