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

- [ ] Storage Access Framework folder sources.
- [ ] Duplicate detection across MediaStore and user folders.
- [ ] Instant metadata search.
- [ ] Media3 playback service and queue.
- [ ] Playlists with M3U import/export.
- [ ] Favourites screen.
- [ ] Artwork cache and lookup pipeline.
- [ ] Bluetooth car device detection and Car Mode.
- [ ] Adaptive layouts for tablets and foldables.
- [ ] Settings import/export and licenses.
