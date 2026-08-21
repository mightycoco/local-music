# Performance Validation

## 50,000-Song Baseline

`LibraryProjectionPerformanceTest` creates 50,000 representative local-library records and measures the production filtering and sorting projection for a filtered, searched, alphabetically sorted result. The JVM acceptance budget is 1,500 ms on a development machine. Run it with:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat testDebugUnitTest --tests com.localmusic.player.ui.home.LibraryProjectionPerformanceTest
```

The library renders Room-backed Paging 3 data through Compose `LazyColumn` rows. The pager loads 100-song pages, prefetches 20 rows ahead, enables placeholders, and caps its in-memory cache at 500 songs, so scrolling does not accumulate the full library in ViewModel or UI state. Artist, album, genre, and folder counts are separate Room aggregation queries over the complete filtered library.

Artwork work is also bounded: the ViewModel prefetches at most 64 visible candidates, and at most four missing covers can request external artwork in one pass. Embedded artwork input is byte-bounded and bitmap decoding is size-limited.

## Query Constraints

Room applies filters, browse values, and supported sort orders before Paging materializes rows. Schema 10 declares sortable text columns with `NOCASE` collation so the composite indexes match production ordering. Indexes cover favourite, play-history, artist/title, album/title, genre/title, folder/title, title/artist, and duration/title paths. Contains search uses bound `LIKE '%query%'` predicates across persisted metadata and therefore cannot use a conventional B-tree prefix lookup.

`LibraryQueryPlanTest` seeds an on-device Room database with 50,000 representative songs, runs `ANALYZE`, and executes `EXPLAIN QUERY PLAN` against the production SQL builders. It verifies every sort order and all artist, album, genre, and folder browse/facet paths use their expected indexes without a full temporary order sort. It also records the `%query%` search table scan as intentional. Run it on a connected device with:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.localmusic.player.data.database.LibraryQueryPlanTest'
```

The three query-plan tests passed on the API 35 `LocalMusicPixel` emulator with 50,000 rows on 2026-08-21. This validates index selection, not UI responsiveness on production hardware.

The remaining empirical work is to capture target-device frame timing and interaction latency while changing search, filters, sort order, browse facets, and scrolling deeply. Record the physical device, build variant, responsiveness threshold, and measured results before marking the performance target complete.