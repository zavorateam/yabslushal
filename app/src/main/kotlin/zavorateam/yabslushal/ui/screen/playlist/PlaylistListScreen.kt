/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import zavorateam.yabslushal.R
import zavorateam.yabslushal.R.string.info_no_playlists_short
import zavorateam.yabslushal.di.LocalBottomBarScrollManager
import zavorateam.yabslushal.di.LocalSizeClass
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.model.DomainSongCollection
import zavorateam.yabslushal.domain.model.settings.BottomBarCollapseMode
import zavorateam.yabslushal.domain.model.settings.BottomBarVisibilityMode
import zavorateam.yabslushal.domain.model.settings.ListViewMode
import zavorateam.yabslushal.playback.MediaPlayer
import zavorateam.yabslushal.ui.component.common.ContentUnavailable
import zavorateam.yabslushal.ui.component.dialog.DeletionDialog
import zavorateam.yabslushal.ui.component.dialog.DeletionTarget
import zavorateam.yabslushal.ui.component.dialog.ShareDialog
import zavorateam.yabslushal.ui.component.layout.ArtGrid
import zavorateam.yabslushal.ui.component.layout.NestedTopBar
import zavorateam.yabslushal.ui.component.layout.PullToRefreshBox
import zavorateam.yabslushal.ui.component.layout.RootBottomBar
import zavorateam.yabslushal.ui.component.layout.RootTopBar
import zavorateam.yabslushal.ui.component.layout.artGridPlaceholder
import zavorateam.yabslushal.ui.component.snackbar.ErrorSnackBar
import zavorateam.yabslushal.ui.core.UiState
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.outlined.Add
import zavorateam.yabslushal.ui.navigation.PersistentViewModelStoreOwner
import zavorateam.yabslushal.ui.screen.playlist.component.PlaylistListScreenGridItem
import zavorateam.yabslushal.ui.screen.playlist.component.PlaylistListScreenListItem
import zavorateam.yabslushal.ui.screen.playlist.component.PlaylistListScreenSortButton
import zavorateam.yabslushal.ui.screen.playlist.dialog.PlaylistCreateDialog
import zavorateam.yabslushal.ui.screen.playlist.viewmodel.PlaylistListViewModel
import zavorateam.yabslushal.ui.util.withoutTop
import zavorateam.yabslushal.ui.viewmodel.RootViewModel

@Composable
fun PlaylistListScreen(
	nested: Boolean = false
) {
	val preferenceManager = koinInject<PreferenceManager>()
	val selectedViewMode = preferenceManager.playlistListViewMode

	val viewModel = koinViewModel<PlaylistListViewModel>(
		viewModelStoreOwner = if (nested) {
			LocalViewModelStoreOwner.current!!
		} else {
			koinInject<PersistentViewModelStoreOwner>()
		}
	)
	val player = koinInject<MediaPlayer>()
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val lazyPagingItems = state.data?.items?.collectAsLazyPagingItems()
	val selectedSorting by viewModel.selectedSorting.collectAsStateWithLifecycle()
	val selectedReversed by viewModel.selectedReversed.collectAsStateWithLifecycle()
	val selectedFilters by viewModel.selectedFilters.collectAsStateWithLifecycle()

	val scrollManager = LocalBottomBarScrollManager.current

	var shareId by remember { mutableStateOf<String?>(null) }
	var deletionId by remember { mutableStateOf<String?>(null) }
	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

	var createDialogShown by rememberSaveable { mutableStateOf(false) }

	val gridState = rememberLazyGridState()

	val actions: @Composable RowScope.() -> Unit = {
		PlaylistListScreenSortButton(
			nested = nested,
			selectedSorting = selectedSorting,
			onSetSorting = { viewModel.setSorting(it) },
			selectedReversed = selectedReversed,
			onSetReversed = { viewModel.setReversed(it) },
			selectedViewMode = selectedViewMode,
			onSetViewMode = { preferenceManager.playlistListViewMode = it },
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
					title = { Text(stringResource(R.string.title_playlists)) },
					scrollBehavior = scrollBehavior,
					actions = actions
				)
			} else {
				NestedTopBar(
					title = { Text(stringResource(R.string.title_playlists)) },
					actions = actions
				)
			}
		},
		floatingActionButton = {
			FloatingActionButton(
				modifier = Modifier.animateFloatingActionButton(
					visible = !scrollManager.isTriggered
						|| preferenceManager.bottomBarCollapseMode == BottomBarCollapseMode.Never,
					alignment = Alignment.BottomEnd
				),
				containerColor = MaterialTheme.colorScheme.primary,
				onClick = { createDialogShown = true }
			) {
				Icon(
					imageVector = Icons.Outlined.Add,
					contentDescription = stringResource(R.string.title_create_playlist),
					modifier = Modifier.size(26.dp)
				)
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
		val itemCount = lazyPagingItems?.itemCount ?: 0

		PullToRefreshBox(
			modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.surface),
			finished = !isLoading,
			onRefresh = { viewModel.refreshPlaylists(true) },
			key = state
		) {
			ArtGrid(
				modifier = if (!nested)
					Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
				else Modifier,
				state = gridState,
				contentPadding = innerPadding.withoutTop(),
				verticalArrangement = if (itemCount == 0 && !isLoading) {
					Arrangement.Center
				} else if (selectedViewMode == ListViewMode.List) {
					Arrangement.spacedBy(0.dp)
				} else {
					Arrangement.spacedBy(12.dp)
				},
				selectedViewMode = selectedViewMode
			) {
				if (itemCount > 0 && lazyPagingItems != null) {
					items(itemCount) { index ->
						val playlist = lazyPagingItems[index] ?: return@items

						if (selectedViewMode == ListViewMode.Grid) {
							PlaylistListScreenGridItem(
								modifier = Modifier.animateItem(),
								tab = "playlists",
								playlist = playlist,
								selected = playlist == state.data?.selected,
								onSelect = {
									viewModel.selected = playlist
								},
								onDeselect = {
									viewModel.clear()
								},
								onSetShareId = {
									shareId = it
								},
								onSetDeletionId = {
									deletionId = it
								},
								onPlayNext = {
									if (state.data?.selected != null) player.playNext(
										state.data?.selected as DomainSongCollection
									)
								},
								onAddToQueue = {
									if (state.data?.selected != null) player.addToQueue(
										state.data?.selected as DomainSongCollection
									)
								},
							)
						} else {
							PlaylistListScreenListItem(
								modifier = Modifier.animateItem(),
								playlist = playlist,
								selected = playlist == state.data?.selected,
								onSelect = {
									viewModel.selected = playlist
								},
								onDeselect = {
									viewModel.clear()
								},
								onSetShareId = {
									shareId = it
								},
								onSetDeletionId = {
									deletionId = it
								},
								onPlayNext = {
									if (state.data?.selected != null) player.playNext(
										state.data?.selected as DomainSongCollection
									)
								},
								onAddToQueue = {
									if (state.data?.selected != null) player.addToQueue(
										state.data?.selected as DomainSongCollection
									)
								},
							)
						}
					}
				} else if (isLoading) {
					artGridPlaceholder(viewMode = selectedViewMode)
				} else {
					item(span = { GridItemSpan(maxLineSpan) }) {
						ContentUnavailable(
							label = stringResource(info_no_playlists_short)
						)
					}
				}
			}
		}
	}

	ErrorSnackBar(
		error = (state as? UiState.Error)?.error,
		onClearError = { viewModel.clearError() }
	)

	ShareDialog(
		id = shareId,
		onDismissRequest = { shareId = null }
	)

	DeletionDialog(
		target = deletionId?.let { DeletionTarget.Playlist(it) },
		onDismissRequest = { deletionId = null },
		onRefresh = { viewModel.refreshPlaylists(true) }
	)

	if (createDialogShown) {
		PlaylistCreateDialog(
			onDismissRequest = { createDialogShown = false },
			onRefresh = { viewModel.refreshPlaylists(true) }
		)
	}
}
