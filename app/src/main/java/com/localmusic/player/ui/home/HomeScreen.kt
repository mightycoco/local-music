package com.localmusic.player.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.localmusic.player.domain.model.LibraryFilter
import com.localmusic.player.domain.model.Song
import com.localmusic.player.domain.model.SortOrder

@Composable
fun HomeRoute(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) viewModel.refreshLibrary()
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) viewModel.refreshLibrary()
    }

    HomeScreen(
        uiState = uiState,
        hasAudioPermission = hasAudioPermission,
        onSearchChange = viewModel::updateSearchQuery,
        onFilterSelected = viewModel::selectFilter,
        onSortSelected = viewModel::selectSortOrder,
        onRequestPermission = { permissionLauncher.launch(audioPermission) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    hasAudioPermission: Boolean,
    onSearchChange: (String) -> Unit,
    onFilterSelected: (LibraryFilter) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onRequestPermission: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Local Music",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Light
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search") }
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (!hasAudioPermission) {
                PermissionBanner(onRequestPermission = onRequestPermission)
                Spacer(modifier = Modifier.height(12.dp))
            }
            uiState.refreshError?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (uiState.isRefreshing) {
                Text(text = "Refreshing local music...", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
            }
            FilterRow(uiState, onFilterSelected, onSortSelected)
            Spacer(modifier = Modifier.height(12.dp))
            SongList(songs = uiState.songs)
        }
    }
}

@Composable
private fun PermissionBanner(onRequestPermission: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Allow audio access to index local songs.",
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = onRequestPermission) {
            Text("Allow Audio Access")
        }
    }
}

@Composable
private fun FilterRow(
    uiState: HomeUiState,
    onFilterSelected: (LibraryFilter) -> Unit,
    onSortSelected: (SortOrder) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryFilter.entries.forEach { filter ->
                AssistChip(
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) },
                    enabled = filter != uiState.selectedFilter
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortOrder.entries.forEach { sortOrder ->
                AssistChip(
                    onClick = { onSortSelected(sortOrder) },
                    label = { Text(sortOrder.label) },
                    enabled = sortOrder != uiState.sortOrder
                )
            }
        }
    }
}

@Composable
private fun SongList(songs: List<Song>) {
    if (songs.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No local songs indexed yet",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "MediaStore and folder scanning will populate this library in the next feature slice.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items = songs, key = { it.id }) { song ->
            ListItem(
                headlineContent = { Text(song.title) },
                supportingContent = { Text("${song.artist} - ${song.album}") }
            )
        }
    }
}
