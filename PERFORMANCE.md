# Performance Validation

## 50,000-Song Baseline

`LibraryProjectionPerformanceTest` creates 50,000 representative local-library records and measures the production filtering and sorting projection for a filtered, searched, alphabetically sorted result. The JVM acceptance budget is 1,500 ms on a development machine. Run it with:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat testDebugUnitTest --tests com.localmusic.player.ui.home.LibraryProjectionPerformanceTest
```

The screen renders results through `LazyColumn`, so composition stays bounded to visible rows. Artwork work is also bounded: the ViewModel prefetches at most 64 visible candidates, and at most four missing covers can request external artwork in one pass. Embedded artwork input is byte-bounded and bitmap decoding is size-limited.

## Current Constraint

Room currently observes the complete song library before the ViewModel applies filtering and sorting. Existing DAO indexes cover date added, scan generation, and URI, which keep scan reconciliation and URI lookup efficient, but they do not provide paging or indexed search/sort queries. The projection benchmark provides a repeatable baseline, not proof that loading a full 50,000-row Room snapshot is ideal.

Before treating the library as validated on a target device, seed or scan 50,000 tracks and capture frame timing while changing search, filters, sort order, and browse facets. If any interaction exceeds the product responsiveness target, replace the full-list observation with SQL-backed filtered paging and add composite indexes for the supported sort/filter combinations.