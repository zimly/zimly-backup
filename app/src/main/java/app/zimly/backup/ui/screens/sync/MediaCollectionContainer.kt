package app.zimly.backup.ui.screens.sync

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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.zimly.backup.data.media.LocalMediaResolver
import app.zimly.backup.data.media.LocalMediaResolverImpl
import app.zimly.backup.ui.screens.sync.MediaCollectionViewModel.Companion.factory
import app.zimly.backup.ui.theme.containerBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Composable
fun MediaCollectionContainer(
    collectionPath: String,
    viewModel: MediaCollectionViewModel = viewModel(factory = factory(collectionPath))
) {

    val folder by viewModel.folderState.collectAsStateWithLifecycle(MediaCollectionState())

    MediaCollection(folder)
}

@Composable
private fun MediaCollection(collectionState: MediaCollectionState) {
    val cardDescription = "Media Collection on Device"
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerBackground(),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = cardDescription
            }
    ) {
        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Outlined.Photo,
                "Media",
                modifier = Modifier
                    .semantics { hideFromAccessibility() }
                    .padding(top = 8.dp, end = 8.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Collection")
                Text(text = collectionState.collection)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Photos")
                Text(text = "${collectionState.photos}")
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
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
    localMediaResolver: LocalMediaResolver,
    collection: String
) : ViewModel() {

    val folderState = snapshotFlow { collection }.map {
        val photoCount = localMediaResolver.photoCount()
        val videoCount = localMediaResolver.videoCount()
        return@map MediaCollectionState(it, photoCount, videoCount)
    }.flowOn(Dispatchers.IO)

    companion object {

        val factory: (collection: String) -> ViewModelProvider.Factory = { collection ->
            viewModelFactory {
                initializer {
                    val application = checkNotNull(this[APPLICATION_KEY])

                    val contentResolver = LocalMediaResolverImpl(application.applicationContext, collection)
                    MediaCollectionViewModel(contentResolver, collection)
                }
            }
        }
    }

}