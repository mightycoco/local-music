package com.localmusic.player.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.SortOrder
import com.localmusic.player.domain.usecase.ObserveSongsUseCase
import com.localmusic.player.domain.usecase.RefreshMusicLibraryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel for library browsing, filtering, sorting, and search state. */
class HomeViewModel(
    observeSongs: ObserveSongsUseCase,
    private val refreshMusicLibrary: RefreshMusicLibraryUseCase
) : ViewModel() {
    private val selectedFilter = MutableStateFlow(LibraryFilter.AllSongs)
    private val sortOrder = MutableStateFlow(SortOrder.NewestAdded)
    private val searchQuery = MutableStateFlow("")
    private val isRefreshing = MutableStateFlow(false)
    private val refreshError = MutableStateFlow<String?>(null)

    private val controlsState = combine(
        selectedFilter,
        sortOrder,
        searchQuery,
        isRefreshing,
        refreshError
    ) { filter, order, query, refreshing, error ->
        HomeUiState(
            selectedFilter = filter,
            sortOrder = order,
            searchQuery = query,
            isRefreshing = refreshing,
            refreshError = error
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        observeSongs(),
        controlsState
    ) { songs, state ->
        state.copy(songs = songs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun selectFilter(filter: LibraryFilter) {
        selectedFilter.value = filter
    }

    fun selectSortOrder(order: SortOrder) {
        sortOrder.value = order
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun refreshLibrary() {
        if (isRefreshing.value) return

        viewModelScope.launch {
            isRefreshing.value = true
            refreshError.value = null
            runCatching { refreshMusicLibrary() }
                .onFailure { error -> refreshError.value = error.message ?: "Library refresh failed" }
            isRefreshing.value = false
        }
    }
}

/** Factory used until a dependency injection container is introduced. */
class HomeViewModelFactory(
    private val observeSongs: ObserveSongsUseCase,
    private val refreshMusicLibrary: RefreshMusicLibraryUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java))
        return HomeViewModel(observeSongs, refreshMusicLibrary) as T
    }
}
