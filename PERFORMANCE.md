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

Room applies filters, browse values, and supported sort orders before Paging materializes rows. Composite indexes cover favourite, play-history, artist/title, album/title, genre/title, folder/title, title/artist, and duration/title paths. Contains search uses bound `LIKE '%query%'` predicates across persisted metadata and therefore cannot use a conventional B-tree prefix lookup.

The remaining validation work is empirical: run `EXPLAIN QUERY PLAN` for every supported query shape against a representative 50,000-row database, then capture target-device frame timing and interaction latency while changing search, filters, sort order, browse facets, and scrolling deeply. Record the device, build variant, responsiveness threshold, and measured results before marking the performance target complete.