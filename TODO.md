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
- [ ] Complete playlist management: open/play playlists, remove entries, reorder entries, and export an individual playlist. Playlist rename and duplication are available from the Playlists screen.
- [x] Add album-art extraction, disk cache pruning, and UI image loading.
- [ ] Complete Car Mode: observe A2DP connection changes, allow marking devices as cars, and apply driving-safe 80-96dp touch targets and simplified controls.
- [ ] Complete Now Playing: show elapsed/remaining time, favourite, shuffle, repeat, swipe navigation, equalizer entry point, and queue contents.
- [x] The selection of the screen must be at the bottom (Home/Now Playing/Playlists/Favourites/Settings)
- [x] The default setting for the theme must be "Follow System", other options are Dark, Light
- [x] The navigation is very slow. The enumeration of media files seems to be done on any UI input
- [x] Keep Now Playing progress and selected song synchronized with Media3 playback state and external media controls.
- [x] Favourite songs must be persisted and retained across library refreshes/app restarts.
- [x] Playlist screen must allow deleting playlists
- [x] Now Playing must allow adding to a new/existing playlist
- [x] Queueing of items must update both the persistent Queue playlist and active Media3 playback queue; Queue cannot be deleted, can be cleared, and is always listed first.
- [x] Playlists must be persisted and loaded after app restart.
- [x] Media list items must have a long press menu with the options to queue, add to a new/existing playlist
- [x] Creating a playlist should just show a modal dialog with a generic TextInput field and Cancel/Create buttons
- [x] The bottom nav buttons must have a larger icon, no text (home, now playing, playlists, favourites, settings)
- [x] The media list filters and search bar should only be visible on the home screen, not on the other screens.
- [ ] Complete offline artwork source priority: embedded artwork, folder.jpg, cover.jpg, then cached artwork.
- [ ] Add an optional external artwork lookup from a free media/artist artwork provider. Request it only after all offline artwork sources fail, cache successful downloads in Android's cache directory, and recover by downloading again when Android clears the cache.
- [ ] Add a default-enabled Settings switch to opt out of external artwork downloading. This exception applies only to artwork metadata; local audio files remain the sole library and playback source, with no streaming or cloud music library support.

## Remaining Prompt Requirements

- [ ] Verify all required local formats on device and document Media3 fallback behavior: MP3, AAC, M4A, FLAC, WAV, AIFF, OGG, and Opus.
- [ ] Complete Home filters: provide meaningful Artists, Albums, Genres, and Folders browsing rather than treating those filter selections as no-ops.
- [ ] Expand the song metadata model and instant search to include album artist, genre, composer, year, comments, description, and playlist names.
- [ ] Add missing smart playlists: Never Played, Last 30 Days, and No Artwork.
- [ ] Complete Settings: artwork cache controls, default filter/sort, folder management, Car Mode and Bluetooth preferences, About/version, and licenses UI.
- [ ] Add accessibility coverage: meaningful semantics, large-font testing, high-contrast behavior, keyboard navigation, and Compose UI tests.
- [ ] Review the adaptive layouts on tablet, foldable, landscape, and square display configurations.
- [ ] Prepare integration boundaries for notification controls, lock-screen controls, home-screen widgets, and Android Auto without implementing the surfaces yet.
- [ ] Validate performance with a 50,000+ song library: pagination or bounded loading, indexed filtering/sorting, bounded artwork work, and measurable UI responsiveness.
- [ ] Complete the requested visual design: Metro-inspired Material 3 styling, large touch targets, responsive portrait/landscape layouts, and purposeful subtle motion.
- [ ] Add explicit dependency-injection composition guidance or a lightweight DI solution once manual wiring no longer scales; keep UI, domain, and data dependencies directional.
- [ ] Review class and function size, KDoc coverage, duplicate UI flows, and Mermaid architecture documentation against the production-quality code standards.
- [ ] Select and integrate a free external artwork provider, such as MusicBrainz and Cover Art Archive, with rate-limit handling, attribution, privacy disclosure, error handling, and cache behavior. It must never supply audio playback or a cloud music library.