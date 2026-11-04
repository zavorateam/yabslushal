/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.collections.immutable.toPersistentList
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zavorateam.yabslushal.R
import zavorateam.yabslushal.di.LocalBottomBarScrollManager
import zavorateam.yabslushal.di.LocalNavStack
import zavorateam.yabslushal.di.LocalSizeClass
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.model.DomainAlbum
import zavorateam.yabslushal.domain.model.DomainArtist
import zavorateam.yabslushal.domain.model.DomainArtistListType
import zavorateam.yabslushal.domain.model.settings.BottomBarVisibilityMode
import zavorateam.yabslushal.playback.MediaPlayer
import zavorateam.yabslushal.ui.component.layout.ArtGridItem
import zavorateam.yabslushal.ui.component.layout.NestedTopBar
import zavorateam.yabslushal.ui.component.layout.PullToRefreshBox
import zavorateam.yabslushal.ui.component.layout.RootBottomBar
import zavorateam.yabslushal.ui.component.layout.RootTopBar
import zavorateam.yabslushal.ui.component.sheet.ArtistSheet
import zavorateam.yabslushal.ui.component.snackbar.ErrorSnackBar
import zavorateam.yabslushal.ui.core.UiState
import zavorateam.yabslushal.ui.navigation.PersistentViewModelStoreOwner
import zavorateam.yabslushal.ui.navigation.Screen
import zavorateam.yabslushal.ui.screen.artist.component.ArtistListScreenContent
import zavorateam.yabslushal.ui.screen.artist.component.ArtistListScreenSortButton
import zavorateam.yabslushal.ui.screen.artist.viewmodel.ArtistListViewModel
import zavorateam.yabslushal.ui.screen.playlist.dialog.PlaylistUpdateDialog
import zavorateam.yabslushal.ui.viewmodel.RootViewModel

@Composable
fun ArtistListScreen(
	nested: Boolean = false,
	listType: DomainArtistListType? = null
) {
	val preferenceManager = koinInject<PreferenceManager>()
	val selectedViewMode = preferenceManager.artistListViewMode

	val viewModel = koinViewModel<ArtistListViewModel>(
		key = listType.toString(),
		parameters = { parametersOf(listType) },
		viewModelStoreOwner = if (nested) {
			LocalViewModelStoreOwner.current!!
		} else {
			koinInject<PersistentViewModelStoreOwner>()
		}
	)
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val lazyPagingItems = state.data?.items?.collectAsLazyPagingItems()

	val selectedArtist = state.data?.selected
	val selectedArtistAlbums = null
	val selectedSorting by viewModel.listType.collectAsStateWithLifecycle()
	val selectedFilters by viewModel.selectedFilters.collectAsStateWithLifecycle()
	val letterCounts by viewModel.letterCounts.collectAsStateWithLifecycle()
	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

	val player = koinInject<MediaPlayer>()

	val actions: @Composable RowScope.() -> Unit = {
		ArtistListScreenSortButton(
			nested = nested,
			selectedSorting = selectedSorting,
			onSetSorting = { viewModel.setListType(it) },
			selectedViewMode = selectedViewMode,
			onSetViewMode = { preferenceManager.artistListViewMode = it },
			selectedFilters = selectedFilters,
			onToggleFilter = { viewModel.toggleFilter(it) }
		)
	}

	val rootViewModel = koinViewModel<RootViewModel>()
	LaunchedEffect(Unit) {
		rootViewModel.events.collect { event ->
			if (event is RootViewModel.Event.ScrollToTop) {
				viewModel.gridState.animateScrollToItem(0)
			}
		}
	}

	Scaffold(
		topBar = {
			if (!nested) {
				RootTopBar(
					{ Text(stringResource(R.string.title_artists)) },
					scrollBehavior,
					actions
				)
			} else {
				NestedTopBar({ Text(stringResource(R.string.title_artists)) }, actions)
			}
		},
		bottomBar = {
			val sizeClass = LocalSizeClass.current
			val scrollManager = LocalBottomBarScrollManager.current
			val preferVisible =
				preferenceManager.bottomBarVisibilityMode == BottomBarVisibilityMode.AllScreens
			if (!nested
				|| (sizeClass.widthSizeClass < WindowWidthSizeClass.Medium && preferVisible)
			) {
				RootBottomBar(scrolled = scrollManager.isTriggered)
			}
		}
	) { innerPadding ->
		val isLoading = state is UiState.Loading || lazyPagingItems?.loadState?.refresh is LoadState.Loading

		PullToRefreshBox(
			modifier = Modifier
				.padding(top = innerPadding.calculateTopPadding())
				.background(MaterialTheme.colorScheme.surface),
			finished = !isLoading,
			onRefresh = { viewModel.refreshArtists(true) },
			key = state
		) {
			ArtistListScreenContent(
				artists = lazyPagingItems,
				isLoading = isLoading,
				letterCounts = letterCounts,
				starred = selectedArtist?.starredAt != null,
				selectedSorting = selectedSorting,
				selectedArtist = selectedArtist,
				selectedArtistAlbums = selectedArtistAlbums,
				selectedViewMode = selectedViewMode,
				gridState = viewModel.gridState,
				scrollBehavior = scrollBehavior,
				innerPadding = innerPadding,
				nested = nested,
				onUpdateSelection = { viewModel.selectArtist(it) },
				onClearSelection = { viewModel.clearSelection() },
				onSetStarred = { viewModel.starArtist(it) },
				onPlayNext = { viewModel.playArtistAlbumsNext(player) },
				onAddToQueue = { viewModel.addArtistAlbumsToQueue(player) }
			)
		}
	}

	ErrorSnackBar(
		error = (state as? UiState.Error)?.error,
		onClearError = { viewModel.clearError() }
	)
}

@Composable
fun ArtistListScreenGridItem(
	modifier: Modifier = Modifier,
	tab: String,
	artist: DomainArtist,
	selected: Boolean,
	selectedArtistAlbums: List<DomainAlbum>?,
	starred: Boolean,
	onSelect: () -> Unit,
	onDeselect: () -> Unit,
	onPlayNext: () -> Unit,
	onAddToQueue: () -> Unit,
	onSetStarred: (starred: Boolean) -> Unit
) {
	val backStack = LocalNavStack.current

	var playlistDialogShown by rememberSaveable { mutableStateOf(false) }

	Box(modifier) {
		ArtGridItem(
			onClick = dropUnlessResumed {
				backStack.add(Screen.ArtistDetail(artist.id))
			},
			onLongClick = onSelect,
			coverArtId = artist.coverArtId,
			title = artist.name,
			subtitle = pluralStringResource(
				R.plurals.count_albums,
				artist.albumCount,
				artist.albumCount
			),
			id = artist.id,
			tab = tab
		)
		if (selected) {
			ArtistSheet(
				onDismissRequest = onDeselect,
				artist = artist,
				onPlayNext = onPlayNext,
				onAddToQueue = onAddToQueue,
				onAddAllToPlaylist = { playlistDialogShown = true },
				starred = starred,
				onSetStarred = { onSetStarred(!starred) }
			)
		}
		if (playlistDialogShown) {
			PlaylistUpdateDialog(
				songIds = selectedArtistAlbums?.flatMap { album -> album.songs.map { it.id } }
					.orEmpty().toPersistentList(),
				onDismissRequest = { playlistDialogShown = false }
			)
		}
	}
}
