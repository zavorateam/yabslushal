/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.starred

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zavorateam.yabslushal.R
import zavorateam.yabslushal.di.LocalBottomBarScrollManager
import zavorateam.yabslushal.di.LocalSizeClass
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.manager.session.DownloadManager
import zavorateam.yabslushal.domain.model.DomainAlbumListType
import zavorateam.yabslushal.domain.model.DomainArtistListType
import zavorateam.yabslushal.domain.model.DomainFilter
import zavorateam.yabslushal.domain.model.DomainSong
import zavorateam.yabslushal.domain.model.DomainSongCollection
import zavorateam.yabslushal.domain.model.DomainSongListType
import zavorateam.yabslushal.domain.model.settings.BottomBarVisibilityMode
import zavorateam.yabslushal.playback.MediaPlayer
import zavorateam.yabslushal.ui.component.dialog.DeleteDownloadTarget
import zavorateam.yabslushal.ui.component.dialog.QueueDuplicateDialog
import zavorateam.yabslushal.ui.component.dialog.ShareDialog
import zavorateam.yabslushal.ui.component.dialog.rememberDownloadDeletionHandler
import zavorateam.yabslushal.ui.component.layout.NestedTopBar
import zavorateam.yabslushal.ui.component.layout.PullToRefreshBox
import zavorateam.yabslushal.ui.component.layout.RootBottomBar
import zavorateam.yabslushal.ui.core.UiState
import zavorateam.yabslushal.ui.navigation.PersistentViewModelStoreOwner
import zavorateam.yabslushal.ui.screen.album.viewmodel.AlbumListViewModel
import zavorateam.yabslushal.ui.screen.artist.viewmodel.ArtistListViewModel
import zavorateam.yabslushal.ui.screen.song.viewmodel.SongListViewModel
import zavorateam.yabslushal.ui.screen.starred.component.StarredScreenContent

@Composable
fun StarredScreen() {
	val persistentViewModelStoreOwner = koinInject<PersistentViewModelStoreOwner>()
	val preferenceManager = koinInject<PreferenceManager>()

	val songsViewModel = koinViewModel<SongListViewModel>(
		key = "starredSongs",
		parameters = {
			parametersOf(
				DomainSongListType.FrequentlyPlayed,
				setOf(DomainFilter.Starred)
			)
		},
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val downloadManager = koinInject<DownloadManager>()
	val songsState by songsViewModel.uiState.collectAsStateWithLifecycle()
	val songsLazyItems = songsState.data?.items?.collectAsLazyPagingItems()
	val allDownloads by downloadManager.allDownloads.collectAsStateWithLifecycle(
		initialValue = emptyList()
	)

	val albumsViewModel = koinViewModel<AlbumListViewModel>(
		key = "starredAlbums",
		parameters = {
			parametersOf(
				DomainAlbumListType.AlphabeticalByArtist,
				setOf(DomainFilter.Starred)
			)
		},
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val albumsState by albumsViewModel.uiState.collectAsStateWithLifecycle()
	val albumsLazyItems = albumsState.data?.items?.collectAsLazyPagingItems()

	val artistsViewModel = koinViewModel<ArtistListViewModel>(
		key = "starredArtists",
		parameters = {
			parametersOf(
				DomainArtistListType.AlphabeticalByName,
				setOf(DomainFilter.Starred)
			)
		},
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val artistsState by artistsViewModel.uiState.collectAsStateWithLifecycle()
	val artistsLazyItems = artistsState.data?.items?.collectAsLazyPagingItems()

	var shareId by rememberSaveable { mutableStateOf<String?>(null) }

	val player = koinInject<MediaPlayer>()

	var songToQueue by remember { mutableStateOf<DomainSong?>(null) }
	val downloadDeletionHandler = rememberDownloadDeletionHandler()

	val isReachable by songsViewModel.isReachable.collectAsStateWithLifecycle()

	Scaffold(
		topBar = { NestedTopBar({ Text(stringResource(R.string.title_starred)) }) },
		bottomBar = {
			val sizeClass = LocalSizeClass.current
			val scrollManager = LocalBottomBarScrollManager.current
			val preferVisible =
				preferenceManager.bottomBarVisibilityMode == BottomBarVisibilityMode.AllScreens
			if (sizeClass.widthSizeClass < WindowWidthSizeClass.Medium && preferVisible) {
				RootBottomBar(scrolled = scrollManager.isTriggered)
			}
		}
	) { innerPadding ->
		val isLoading = songsState is UiState.Loading ||
			albumsState is UiState.Loading ||
			artistsState is UiState.Loading ||
			songsLazyItems?.loadState?.refresh is LoadState.Loading ||
			albumsLazyItems?.loadState?.refresh is LoadState.Loading ||
			artistsLazyItems?.loadState?.refresh is LoadState.Loading

		PullToRefreshBox(
			modifier = Modifier
				.padding(top = innerPadding.calculateTopPadding())
				.background(MaterialTheme.colorScheme.surface),
			finished = !isLoading,
			onRefresh = {
				albumsViewModel.refreshAlbums(true)
				artistsViewModel.refreshArtists(true)
				songsViewModel.refreshSongs(true)
			},
			key = listOf(albumsState, artistsState, songsState)
		) {
			val selectedAlbum = albumsState.data?.selected
			val selectedArtist = artistsState.data?.selected

			StarredScreenContent(
				innerPadding = innerPadding,
				isLoading = isLoading,
				onSetShareId = { shareId = it },
				isOnline = isReachable,

				songs = songsLazyItems,
				selectedSong = songsState.data?.selected,
				allDownloads = allDownloads,
				onPlaySong = { index ->
					songsLazyItems?.itemSnapshotList?.items?.let {
						player.playNow(it, index)
					}
				},
				onSelectSong = {
					songsViewModel.selected = it
				},
				onClearSongSelection = { songsViewModel.clear() },
				selectedSongIsStarred = songsState.data?.selected?.starredAt != null,
				onAddSongStar = { songsViewModel.starSong(true) },
				onRemoveSongStar = { songsViewModel.starSong(false) },
				onDownloadSong = { downloadManager.downloadSong(it.id) },
				onCancelDownloadSong = { song ->
					downloadManager.cancelDownload(song.id)
				},
				onDeleteDownloadSong = { song ->
					downloadDeletionHandler.requestDelete(DeleteDownloadTarget.Single(song.id, song.title))
				},
				onPlaySongNext = { song ->
					if (player.checkDuplicate(song.id, preferenceManager)) {
						songToQueue = song
					} else {
						player.playNextSingle(song)
					}
				},
				onAddSongToQueue = { song ->
					if (player.checkDuplicate(song.id, preferenceManager)) {
						songToQueue = song
					} else {
						player.addToQueueSingle(song)
					}
				},
				selectedSongRating = songsState.data?.selected?.userRating ?: 0,
				onSetSongRating = { songsViewModel.rateSelectedSong(it) },

				albums = albumsLazyItems,
				selectedAlbum = selectedAlbum,
				selectedAlbumIsStarred = selectedAlbum?.starredAt != null,
				selectedAlbumRating = albumsState.data?.selected?.userRating ?: 0,
				onSelectAlbum = {
					albumsViewModel.selected = it
				},
				onClearAlbumSelection = {
					albumsViewModel.selected = null
				},
				onStarSelectedAlbum = { albumsViewModel.starAlbum(it) },
				onPlayAlbumNext = { if (selectedAlbum != null) player.playNext(selectedAlbum as DomainSongCollection) },
				onAddAlbumToQueue = { if (selectedAlbum != null) player.addToQueue(selectedAlbum as DomainSongCollection) },
				onRateSelectedAlbum = { albumsViewModel.setRating(it) },

				artists = artistsLazyItems,
				selectedArtist = artistsState.data?.selected,
				// TODO: artist albums
				selectedArtistAlbums = emptyList(),
				selectedArtistIsStarred = false,
				onSelectArtist = { artistsViewModel.selectArtist(it) },
				onClearArtistSelection = { artistsViewModel.clearSelection() },
				onStarSelectedArtist = { artistsViewModel.starArtist(it) },
				onPlayArtistNext = {
					if (selectedArtist != null) artistsViewModel.playArtistAlbumsNext(
						player
					)
				},
				onAddArtistToQueue = {
					if (selectedArtist != null) artistsViewModel.addArtistAlbumsToQueue(
						player
					)
				},
			)
		}
	}

	ShareDialog(
		id = shareId,
		onDismissRequest = { shareId = null }
	)

	QueueDuplicateDialog(
		song = songToQueue,
		onDismissRequest = { songToQueue = null }
	)
}
