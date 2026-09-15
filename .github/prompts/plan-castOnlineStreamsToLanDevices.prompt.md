## Plan: Cast Online Streams to LAN Devices

Add Google Cast sender support for current HTTP(S) radio streams, using the official Cast framework and its Default Media Receiver so compatible Chromecast built-in devices such as the Harman Kardon Citation Oasis fetch the stream directly. Keep local playback running during discovery/loading, pause it only after the receiver reports the same stream as `PLAYING`, retain remote play/pause and explicit stop-casting control, and never resume local playback automatically.

**Steps**

### Phase 1: Cast foundation and physical feasibility
1. Add a narrowly documented dependency-policy exception for the proprietary Google Cast sender SDK. Add the current verified `play-services-cast-framework` version to the version catalog and app dependencies, while retaining the open-source-only rule for unrelated dependencies.
2. Configure Google Cast with an `OptionsProvider` using `CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID`, and register it through application metadata. Use the Default Media Receiver so no custom receiver registration or app ID is required.
3. Build the smallest device spike before deeper UI work: expose a temporary/isolated standard Cast route button, verify the Citation Oasis appears on the same LAN, and load one known-good public HTTPS MP3 followed by one stream URI already saved by the app. Confirm the receiver reaches `BUFFERING` then `PLAYING`, audio is audible, and playback continues when the sender is backgrounded. Stop here and record a blocker if the physical Oasis is not discoverable or rejects the real station URL; do not add a speculative DLNA stack.

### Phase 2: Domain boundary and Cast state machine
4. Add protocol-neutral immutable models for a remote stream route and transfer state. The state must distinguish `Unavailable`, `Available`, `Connecting`, `Buffering`, `Playing`, `Paused`, and `Error`, and carry the selected device plus the requested song ID/URI so delayed callbacks cannot affect a newer local selection.
5. Add a dedicated `RemoteStreamController` domain contract beside `PlaybackController`, with an observable state and commands to load the current stream, play/pause the remote, stop playback and end the Cast session, and release callbacks. Keep Google SDK types out of domain and Compose.
6. Implement `GoogleCastRemoteStreamController` in a focused `cast` package. Let the Cast framework own mDNS discovery/session lifecycle and the standard route chooser; do not implement raw LAN scanning. Build a live `MediaInfo` from the existing HTTP(S) `Song.uri`, `Song.mimeType` (falling back to a conservative audio MIME type), title/artist/artwork metadata, and autoplay. Translate session, `RemoteMediaClient`, load-result, `MediaStatus`, `MediaError`, and idle-reason callbacks into the domain state.
7. Make the transfer state machine generation/request aware. A load-command success is only acceptance, not completed handoff. Declare transfer success only when the active Cast session reports `PLAYER_STATE_PLAYING` for the requested URI. Treat disconnect, idle error, media error, or session failure before that point as a failed transfer and leave local playback unchanged.
8. Add focused JVM tests with a fake Cast gateway/controller for discovery/session/load transitions, MIME fallback, rejected loads, disconnects, errors, stale callbacks, and exactly-once confirmation of remote `PLAYING` for the current request.

### Phase 3: ViewModel orchestration
9. Assemble the remote controller in `MainActivity` and pass it through the existing feature-owned dependency bundle/`HomeViewModelFactory` constructor wiring. Observe its state in `HomeViewModel` and project only the immutable remote-stream state needed by Now Playing.
10. Add `HomeViewModel` commands for starting a handoff, controlling remote play/pause, and stopping casting. Permit handoff only when the current song is `SongSource.STREAM` and its URI is HTTP or HTTPS. Keep local Media3 playback active during route selection, Cast connection, load acceptance, and buffering.
11. On the first matching transition to remote `Playing`, call the existing local playback `pause()` once and mark controls as remote-owned. Do not clear the persisted Queue or release the Media3 controller. On transfer failure, show a scoped Cast error and preserve local playback. On remote stop, user disconnect, network loss, app restart, or session loss, leave local playback paused; resumption must be explicit.
12. While remote-owned, route the main play/pause action to Cast and derive its icon/state from remote status. Disable seek, Previous, Next, queue-row selection, shuffle, repeat, and any command that would imply remote queue support. The first release casts one current live stream only. Explicit Stop casting must issue remote stop before ending the Cast session and must not resume the phone.
13. Add orchestration tests around a fake `RemoteStreamController`: button eligibility, local playback continuing through connecting/buffering, pause only after matching remote `Playing`, no pause on failure/stale callback, remote play/pause routing, disabled queue operations, exactly-once local pause, and no automatic resume after stop/disconnect.

### Phase 4: Now Playing experience
14. Add the standard Google Cast `MediaRouteButton`, visually presented as the Stream control, to Now Playing transport controls only when the current item is an eligible online `SongSource.STREAM`. Do not show it for MediaStore/SAF/local items, unsupported URI schemes, the mini-player, or other screens. Configure it with `CastButtonFactory`; preserve the framework route chooser and accessibility behavior rather than implementing a custom device list.
15. Reflect Cast lifecycle without overlapping controls: inactive/available, connecting/buffering progress, active device name, and a scoped error/retry state. When active, retain the main remote play/pause control and expose a clear Stop casting action through the connected Stream control/dialog. Disable Previous/Next and queue selection with correct semantics/content descriptions rather than allowing no-op callbacks.
16. Add Compose/instrumentation coverage for Stream-button eligibility, connecting/active/error presentation, remote play/pause state, disabled unsupported controls, and Stop casting. Verify portrait, short landscape, and Car Mode sizing so the added control does not shift or overlap existing 64/72 dp transport targets.

### Phase 5: Documentation and release verification
17. Update architecture documentation to show `RemoteStreamController` as a separate Cast boundary from local Media3 playback and explain receiver-side URL fetching, the handoff success criterion, local-pause timing, and the single-current-stream limitation.
18. Add a TODO checklist for Google Cast discovery/handoff/UI/device validation and check items only as they are completed. Add a concise Unreleased changelog entry and update README product/dependency language to describe online-radio casting and the explicit Google Cast SDK exception without implying local-file casting.
19. Run focused JVM tests for the Cast state machine and ViewModel orchestration, Compose/instrumentation tests for Now Playing, then the full unit test suite and `Gradle: Assemble Debug APK` task.
20. Perform final physical validation on the Citation Oasis: discover it on the LAN; hand off a known MP3 and representative app streams; verify only the radio fetches data; verify local audio continues until remote `PLAYING` and then pauses without overlap; exercise remote play/pause and Stop casting; reject/timeout a bad URL without stopping local audio; disconnect Wi-Fi/end the session without local auto-resume; background/foreground the app; and verify local songs never show Stream.

**Relevant files**
- `c:/etc/git/local-music/gradle/libs.versions.toml` — declare the Cast framework version and alias.
- `c:/etc/git/local-music/app/build.gradle.kts` — add the Cast sender dependency.
- `c:/etc/git/local-music/app/src/main/AndroidManifest.xml` — register the Cast `OptionsProvider` metadata; add only permissions proven necessary by the SDK/target Android versions.
- `c:/etc/git/local-music/app/src/main/java/com/localmusic/player/cast/LocalMusicCastOptionsProvider.kt` — new Default Media Receiver configuration.
- `c:/etc/git/local-music/app/src/main/java/com/localmusic/player/cast/GoogleCastRemoteStreamController.kt` — new SDK adapter, media load, callback lifecycle, and state translation.
- `c:/etc/git/local-music/app/src/main/java/com/localmusic/player/domain/model/RemoteStreamState.kt` — new protocol-neutral route and transfer state.
- `c:/etc/git/local-music/app/src/main/java/com/localmusic/player/domain/repository/RemoteStreamController.kt` — new remote-stream control boundary.
- `c:/etc/git/local-music/app/src/main/java/com/localmusic/player/MainActivity.kt` — construct and inject the controller using existing explicit composition.
- `c:/etc/git/local-music/app/src/main/java/com/localmusic/player/ui/home/HomeViewModel.kt` — coordinate transfer confirmation, local pause, remote controls, errors, stale-event guards, and lifecycle cleanup.
- `c:/etc/git/local-music/app/src/main/java/com/localmusic/player/ui/home/HomeUiState.kt` — project immutable Cast state/capabilities into Now Playing state (or the existing feature-owned Now Playing substate if split in the current tree).
- `c:/etc/git/local-music/app/src/main/java/com/localmusic/player/ui/home/HomeActions.kt` and `c:/etc/git/local-music/app/src/main/java/com/localmusic/player/ui/home/PlaybackActions.kt` — add focused remote-stream actions without exposing SDK objects.
- `c:/etc/git/local-music/app/src/main/java/com/localmusic/player/ui/home/HomeScreen.kt` — wire ViewModel actions and standard Cast route-button setup at the Android/Compose boundary.
- `c:/etc/git/local-music/app/src/main/java/com/localmusic/player/ui/home/NowPlayingScreen.kt` — conditional Stream control, state/status UI, remote play/pause, Stop casting, and disabled unsupported controls.
- `c:/etc/git/local-music/app/src/test/java/com/localmusic/player/cast/GoogleCastRemoteStreamControllerTest.kt` — new state-machine and callback-race tests through fakes.
- `c:/etc/git/local-music/app/src/test/java/com/localmusic/player/ui/home/RemoteStreamHandoffTest.kt` — new orchestration/business-rule tests.
- `c:/etc/git/local-music/app/src/androidTest/java/com/localmusic/player/ui/home/NowPlayingCastTest.kt` — new conditional/control-state UI tests where SDK isolation permits deterministic fakes.
- `c:/etc/git/local-music/ARCHITECTURE.md`, `c:/etc/git/local-music/TODO.md`, `c:/etc/git/local-music/CHANGELOG.md`, and `c:/etc/git/local-music/README.md` — architecture, tracked work, user-visible change, scope, and dependency-policy documentation.

**Verification**
1. Physical feasibility gate: Citation Oasis appears in the official Cast chooser and plays both a known public HTTPS MP3 and a real saved app stream through the Default Media Receiver.
2. Focused automated checks: state-machine and ViewModel tests prove matching-URI/one-shot pause semantics, failure preservation, stale callback rejection, remote command routing, and no auto-resume.
3. UI checks: Stream appears only for eligible HTTP(S) `SongSource.STREAM` items; remote lifecycle and disabled controls remain usable in portrait, short landscape, and Car Mode.
4. Regression checks: full unit tests, relevant instrumentation tests, and debug APK assembly pass.
5. Device behavior: no local/remote overlap after confirmed start, no premature local stop during buffering/failure, remote playback survives sender backgrounding, and Stop/disconnect leaves local paused.

**Decisions**
- First-release devices: Google Cast / Chromecast built-in only, including Citation Oasis. DLNA, UPnP, OpenHome, AirPlay, Bluetooth, and raw mDNS/SSDP implementations are excluded.
- Receiver: Google Cast Default Media Receiver; no custom receiver registration or app ID.
- Dependency policy: allow and document one narrow exception for the proprietary Google Play services Cast framework because reliable Citation support is otherwise infeasible.
- Castable content: only existing online HTTP(S) songs with `SongSource.STREAM`. Local MediaStore/SAF files, phone-hosted HTTP serving, authenticated/header-dependent streams, and arbitrary web pages are excluded.
- Transfer timing: local playback pauses only after the receiver reports the requested stream as `PLAYING`, not when a device connects or a load request is accepted.
- Remote scope: one current live stream with play/pause and Stop casting. No remote queue, seek, Previous/Next, shuffle, repeat, automatic failover, or automatic local resume.
- Discovery UI: official Cast route chooser and `MediaRouteButton`, labeled/presented as Stream in Now Playing, to preserve Cast discovery/session UX and accessibility.

**Further Considerations**
1. Android 16+ local-network permission behavior should be verified against the implementation-time SDK and Cast framework release; request or declare permissions only when official Cast guidance and device testing require them.
2. Some station URLs may fail receiver-side because of codecs, redirects, TLS, CORS, expiring tokens, cookies, or custom headers. Surface those as Cast errors; do not proxy them through the phone in this scope.
