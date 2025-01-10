package app.zimly.backup.ui.screens.sync

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.data.media.LocalMediaRepository
import app.zimly.backup.ui.screens.sync.SyncViewModel.SyncConfigurationState
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Composable
fun MediaCollectionCompose(
    syncConfigurationFlow: Flow<SyncConfigurationState>,
    application: Application,
    viewModel: MediaCollectionViewModel = viewModel(factory = viewModelFactory {
        initializer {
            val mediaRepository = LocalMediaRepository(application.contentResolver)
            MediaCollectionViewModel(mediaRepository, syncConfigurationFlow)
        }
    }),
) {

    val folder by viewModel.folderState.collectAsStateWithLifecycle(MediaCollectionState())

    MediaCollection(folder)
}

@Composable
private fun MediaCollection(collectionState: MediaCollectionState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerBackground(),
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Outlined.Photo,
                "Media",
                modifier = Modifier.padding(top = 8.dp, end = 8.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Collection")
                Text(text = collectionState.collection)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Photos")
                Text(text = "${collectionState.photos}")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Videos")
                Text(text = "${collectionState.videos}")
            }

        }
    }
}

data class MediaCollectionState(
    var collection: String = "",
    var photos: Int = 0,
    var videos: Int = 0,
)

class MediaCollectionViewModel(
    mediaRepository: LocalMediaRepository,
    syncConfigurationFlow: Flow<SyncConfigurationState>
) : ViewModel() {

    val folderState = syncConfigurationFlow.map {
        val photoCount = mediaRepository.getPhotos(setOf(it.sourceUri)).size
        val videoCount = mediaRepository.getVideos(setOf(it.sourceUri)).size
        return@map MediaCollectionState(it.sourceUri, photoCount, videoCount)
    }
}