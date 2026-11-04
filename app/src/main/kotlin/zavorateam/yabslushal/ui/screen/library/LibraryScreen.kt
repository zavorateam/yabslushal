/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zavorateam.yabslushal.R
import zavorateam.yabslushal.di.LocalBottomBarScrollManager
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.manager.session.LoginManager
import zavorateam.yabslushal.domain.model.DomainAlbumListType
import zavorateam.yabslushal.domain.model.DomainArtistListType
import zavorateam.yabslushal.domain.model.DomainSongCollection
import zavorateam.yabslushal.playback.MediaPlayer
import zavorateam.yabslushal.ui.component.dialog.DeletionDialog
import zavorateam.yabslushal.ui.component.dialog.DeletionTarget
import zavorateam.yabslushal.ui.component.dialog.ShareDialog
import zavorateam.yabslushal.ui.component.layout.PullToRefreshBox
import zavorateam.yabslushal.ui.component.layout.RootBottomBar
import zavorateam.yabslushal.ui.component.layout.RootTopBar
import zavorateam.yabslushal.ui.component.snackbar.ErrorSnackBar
import zavorateam.yabslushal.ui.core.LoginUiState
import zavorateam.yabslushal.ui.core.UiState
import zavorateam.yabslushal.ui.navigation.PersistentViewModelStoreOwner
import zavorateam.yabslushal.ui.screen.album.viewmodel.AlbumListViewModel
import zavorateam.yabslushal.ui.screen.artist.viewmodel.ArtistListViewModel
import zavorateam.yabslushal.ui.screen.genre.viewmodel.GenreListViewModel
import zavorateam.yabslushal.ui.screen.library.component.LibraryScreenContent
import zavorateam.yabslushal.ui.screen.playlist.dialog.PlaylistCreateDialog
import zavorateam.yabslushal.ui.screen.playlist.viewmodel.PlaylistListViewModel
import zavorateam.yabslushal.ui.screen.stats.viewmodel.StatisticsViewModel
import zavorateam.yabslushal.ui.viewmodel.RootViewModel

@Composable
fun LibraryScreen() {
	val persistentViewModelStoreOwner = koinInject<PersistentViewModelStoreOwner>()

	val albumsViewModel = koinViewModel<AlbumListViewModel>(
		key = "libraryAlbums",
		parameters = { parametersOf(DomainAlbumListType.Recent) },
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val albumsState by albumsViewModel.uiState.collectAsStateWithLifecycle()
	val albumsLazyItems = albumsState.data?.items?.collectAsLazyPagingItems() ?: return

	val playlistsViewModel = koinViewModel<PlaylistListViewModel>(
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val playlistsState by playlistsViewModel.uiState.collectAsStateWithLifecycle()
	val playlistsLazyItems = playlistsState.data?.items?.collectAsLazyPagingItems() ?: return

	val artistsViewModel = koinViewModel<ArtistListViewModel>(
		key = "libraryArtists",
		parameters = { parametersOf(DomainArtistListType.AlphabeticalByName) },
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val artistsState by artistsViewModel.uiState.collectAsStateWithLifecycle()
	val artistsLazyItems = artistsState.data?.items?.collectAsLazyPagingItems() ?: return

	val genresViewModel = koinViewModel<GenreListViewModel>(
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val genresState by genresViewModel.uiState.collectAsStateWithLifecycle()
	val genresLazyItems = genresState.data?.collectAsLazyPagingItems() ?: return

	val statsViewModel = koinViewModel<StatisticsViewModel>(
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val statsState by statsViewModel.state.collectAsStateWithLifecycle()

	val loginManager = koinInject<LoginManager>()
	val loginState by loginManager.loginState.collectAsStateWithLifecycle()

	var shareId by rememberSaveable { mutableStateOf<String?>(null) }
	var playlistDeletionId by rememberSaveable { mutableStateOf<String?>(null) }
	var playlistCreateDialogShown by rememberSaveable { mutableStateOf(false) }

	val player = koinInject<MediaPlayer>()

	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

	// Keeping the layout inside composition keeps the grid in sync with the editor:
	// any change made in Settings instantly re-renders the sections.
	val preferenceManager = koinInject<PreferenceManager>()
	val libraryLayout = preferenceManager.libraryLayout

	LaunchedEffect(loginState is LoginUiState.Success) {
		albumsViewModel.refreshAlbums(false)
		playlistsViewModel.refreshPlaylists(false)
		artistsViewModel.refreshArtists(false)
		genresViewModel.refreshGenres(false)
	}

	val gridState = rememberLazyGridState()
	val rootViewModel = koinViewModel<RootViewModel>()
	LaunchedEffect(Unit) {
		rootViewModel.events.collect { event ->
			if (event is RootViewModel.Event.ScrollToTop) {
				gridState.animateScrollToItem(0)
			}
		}
	}

	Scaffold(
		topBar = { RootTopBar({ Text(stringResource(R.string.title_library)) }, scrollBehavior) },
		bottomBar = {
			val scrollManager = LocalBottomBarScrollManager.current
			RootBottomBar(scrolled = scrollManager.isTriggered)
		}
	) { innerPadding ->
		PullToRefreshBox(
			modifier = Modifier
				.padding(top = innerPadding.calculateTopPadding())
				.background(MaterialTheme.colorScheme.surface),
			finished = albumsState !is UiState.Loading &&
				playlistsState !is UiState.Loading &&
				artistsState !is UiState.Loading &&
				genresState !is UiState.Loading,
			onRefresh = {
				albumsViewModel.refreshAlbums(true)
				playlistsViewModel.refreshPlaylists(true)
				artistsViewModel.refreshArtists(true)
				genresViewModel.refreshGenres(true)
			},
			key = listOf(albumsState, playlistsState, artistsState, genresState)
		) {
			val selectedAlbum = albumsState.data?.selected
			val selectedArtist = artistsState.data?.selected
			val selectedPlaylist = playlistsState.data?.selected

			LibraryScreenContent(
				state = gridState,
				scrollBehavior = scrollBehavior,
				innerPadding = innerPadding,
				onSetShareId = { shareId = it },

				albumsState = albumsLazyItems,
				selectedAlbum = albumsState.data?.selected,
				selectedAlbumIsStarred = selectedAlbum?.starredAt != null,
				selectedAlbumRating = selectedAlbum?.userRating ?: 0,
				onSelectAlbum = {
					albumsViewModel.selected = it
				},
				onClearAlbumSelection = {
					albumsViewModel.selected = null
				},
				onStarSelectedAlbum = { albumsViewModel.starAlbum(it) },
				onPlayAlbumNext = { selectedAlbum?.let { player.playNext(it as DomainSongCollection) } },
				onAddAlbumToQueue = { selectedAlbum?.let { player.addToQueue(selectedAlbum as DomainSongCollection) } },
				onRateSelectedAlbum = { albumsViewModel.setRating(it) },

				artistsState = artistsLazyItems,
				selectedArtist = selectedArtist,
				selectedArtistAlbums = null,
				selectedArtistIsStarred = artistsState.data?.selected?.starredAt != null,
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

				playlistsState = playlistsLazyItems,
				selectedPlaylist = selectedPlaylist,
				onSelectPlaylist = {
					playlistsViewModel.selected = it
				},
				onClearPlaylistSelection = { playlistsViewModel.clear() },
				onDeletePlaylist = { playlistDeletionId = it },
				onPlayPlaylistNext = {
					if (selectedPlaylist != null) player.playNext(
						selectedPlaylist as DomainSongCollection
					)
				},
				onAddPlaylistToQueue = {
					if (selectedPlaylist != null) player.addToQueue(
						selectedPlaylist as DomainSongCollection
					)
				},

				genresState = genresLazyItems,
				statsState = statsState,

				sectionOrder = libraryLayout.sections
			)
		}
	}

	val flattenedErrors = listOf(
		(albumsState as? UiState.Error)?.error,
		(playlistsState as? UiState.Error)?.error,
		(artistsState as? UiState.Error)?.error,
		(genresState as? UiState.Error)?.error
	).mapNotNull { it?.stackTraceToString() }.takeIf { it.isNotEmpty() }?.joinToString("\n\n")

	ErrorSnackBar(
		error = flattenedErrors?.let { Error(it) },
		onClearError = {
			albumsViewModel.clearError()
			playlistsViewModel.clearError()
			artistsViewModel.clearError()
			genresViewModel.clearError()
		}
	)

	ShareDialog(
		id = shareId,
		onDismissRequest = { shareId = null }
	)

	DeletionDialog(
		target = playlistDeletionId?.let { DeletionTarget.Playlist(it) },
		onDismissRequest = { playlistDeletionId = null },
		onRefresh = { playlistsViewModel.refreshPlaylists(false) }
	)

	if (playlistCreateDialogShown) {
		PlaylistCreateDialog(
			onDismissRequest = { playlistCreateDialogShown = false },
			onRefresh = { playlistsViewModel.refreshPlaylists(true) }
		)
	}
}
