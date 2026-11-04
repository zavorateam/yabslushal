/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zavorateam.yabslushal.R
import zavorateam.yabslushal.R.string.info_no_albums
import zavorateam.yabslushal.di.LocalBottomBarScrollManager
import zavorateam.yabslushal.di.LocalSizeClass
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.model.DomainAlbumListType
import zavorateam.yabslushal.domain.model.DomainSongCollection
import zavorateam.yabslushal.domain.model.settings.BottomBarVisibilityMode
import zavorateam.yabslushal.domain.model.settings.ListViewMode
import zavorateam.yabslushal.playback.MediaPlayer
import zavorateam.yabslushal.ui.component.common.AlphabeticalScroller
import zavorateam.yabslushal.ui.component.common.ContentUnavailable
import zavorateam.yabslushal.ui.component.dialog.ShareDialog
import zavorateam.yabslushal.ui.component.layout.ArtGrid
import zavorateam.yabslushal.ui.component.layout.NestedTopBar
import zavorateam.yabslushal.ui.component.layout.PullToRefreshBox
import zavorateam.yabslushal.ui.component.layout.RootBottomBar
import zavorateam.yabslushal.ui.component.layout.RootTopBar
import zavorateam.yabslushal.ui.component.layout.artGridPlaceholder
import zavorateam.yabslushal.ui.component.snackbar.ErrorSnackBar
import zavorateam.yabslushal.ui.core.UiState
import zavorateam.yabslushal.ui.navigation.PersistentViewModelStoreOwner
import zavorateam.yabslushal.ui.screen.album.component.AlbumListScreenGridItem
import zavorateam.yabslushal.ui.screen.album.component.AlbumListScreenListItem
import zavorateam.yabslushal.ui.screen.album.component.AlbumListScreenSortButton
import zavorateam.yabslushal.ui.screen.album.viewmodel.AlbumListViewModel
import zavorateam.yabslushal.ui.util.withoutTop
import zavorateam.yabslushal.ui.viewmodel.RootViewModel

@Composable
fun AlbumListScreen(
	nested: Boolean = false,
	listType: DomainAlbumListType? = null
) {
	val preferenceManager = koinInject<PreferenceManager>()
	val selectedViewMode = preferenceManager.albumListViewMode

	val viewModel = koinViewModel<AlbumListViewModel>(
		key = listType.toString(),
		parameters = { parametersOf(listType) },
		viewModelStoreOwner = if (nested) {
			LocalViewModelStoreOwner.current!!
		} else {
			koinInject<PersistentViewModelStoreOwner>()
		}
	)
	val player = koinInject<MediaPlayer>()
	val selectedSorting by viewModel.listType.collectAsStateWithLifecycle()
	val selectedReversed by viewModel.selectedReversed.collectAsStateWithLifecycle()
	val selectedFilters by viewModel.selectedFilters.collectAsStateWithLifecycle()
	val letterCounts by viewModel.letterCounts.collectAsStateWithLifecycle()
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val lazyPagingItems = state.data?.items?.collectAsLazyPagingItems()

	val selectedAlbum = state.data?.selected
	var shareId by remember { mutableStateOf<String?>(null) }
	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

	val actions: @Composable RowScope.() -> Unit = {
		AlbumListScreenSortButton(
			nested = nested,
			selectedSorting = selectedSorting,
			onSetSorting = { viewModel.setListType(it) },
			selectedReversed = selectedReversed,
			onSetReversed = { viewModel.setReversed(it) },
			selectedViewMode = selectedViewMode,
			onSetViewMode = { preferenceManager.albumListViewMode = it },
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
					{ Text(stringResource(R.string.title_albums)) },
					scrollBehavior,
					actions
				)
			} else {
				NestedTopBar({ Text(stringResource(R.string.title_albums)) }, actions)
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
			onRefresh = { viewModel.refreshAlbums(true) },
			key = state
		) {
			val headerIndices = remember(letterCounts) {
				var gridPosition = 0
				val list = mutableListOf<Pair<String, Int>>()
				letterCounts.forEach { lc ->
					list.add(lc.letter to gridPosition)
					gridPosition += 1 + lc.count
				}
				list.toImmutableList()
			}
			Box {
				ArtGrid(
					modifier = if (!nested)
						Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
					else Modifier,
					state = viewModel.gridState,
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
						if (selectedSorting == DomainAlbumListType.AlphabeticalByName) {
							val textPadding = PaddingValues(
								horizontal = if (selectedViewMode == ListViewMode.List) 16.dp else 0.dp,
								vertical = 8.dp
							)

							var albumOffset = 0
							letterCounts.forEach { lc ->
								stickyHeader {
									Row(
										modifier = Modifier
											.background(MaterialTheme.colorScheme.surface)
											.padding(textPadding),
										verticalAlignment = Alignment.CenterVertically
									) {
										Text(
											text = lc.letter,
											color = MaterialTheme.colorScheme.onSurfaceVariant
										)
									}
								}
								val startAlbumIndex = albumOffset
								items(lc.count) { i ->
									val albumIndex = startAlbumIndex + i
									if (albumIndex >= lazyPagingItems.itemCount) return@items
									val album = lazyPagingItems[albumIndex] ?: return@items

									if (selectedViewMode == ListViewMode.Grid) {
										AlbumListScreenGridItem(
											modifier = Modifier.animateItem(),
											tab = "albums",
											album = album,
											selected = album == selectedAlbum,
											starred = selectedAlbum?.starredAt != null,
											onSelect = {
												viewModel.selected = album
											},
											onDeselect = {
												viewModel.selected = null
											},
											onSetStarred = {
												viewModel.starAlbum(it)
											},
											onSetShareId = {
												shareId = it
											},
											onPlayNext = {
												if (selectedAlbum != null) player.playNext(
													selectedAlbum as DomainSongCollection
												)
											},
											onAddToQueue = {
												if (selectedAlbum != null) {
													player.addToQueue(
														selectedAlbum as DomainSongCollection
													)
												}
											},
											rating = selectedAlbum?.userRating ?: 0,
											onSetRating = { it: Int -> viewModel.setRating(it) }
										)
									} else {
										AlbumListScreenListItem(
											modifier = Modifier.animateItem(),
											album = album,
											selected = album == selectedAlbum,
											starred = selectedAlbum?.starredAt != null,
											onSelect = {
												viewModel.selected = album
											},
											onDeselect = {
												viewModel.selected = null
											},
											onSetStarred = {
												viewModel.starAlbum(it)
											},
											onSetShareId = { newShareId: String ->
												shareId = newShareId
											},
											onPlayNext = {
												if (selectedAlbum != null) player.playNext(
													selectedAlbum as DomainSongCollection
												)
											},
											onAddToQueue = {
												if (selectedAlbum != null) {
													player.addToQueue(
														selectedAlbum as DomainSongCollection
													)
												}
											},
											rating = selectedAlbum?.userRating ?: 0,
											onSetRating = { it: Int -> viewModel.setRating(it) }
										)
									}
								}
								albumOffset += lc.count
							}
						} else {
							items(
								lazyPagingItems.itemCount,
								key = { index -> lazyPagingItems.peek(index)?.id ?: index }) { index ->
								val album = lazyPagingItems[index] ?: return@items

								if (selectedViewMode == ListViewMode.Grid) {
									AlbumListScreenGridItem(
										modifier = Modifier.animateItem(),
										tab = "albums",
										album = album,
										selected = album == selectedAlbum,
										starred = selectedAlbum?.starredAt != null,
										onSelect = {
											viewModel.selected = album
										},
										onDeselect = {
											viewModel.selected = null
										},
										onSetStarred = {
											viewModel.starAlbum(it)
										},
										onSetShareId = {
											shareId = it
										},
										onPlayNext = {
											if (selectedAlbum != null) player.playNext(
												selectedAlbum as DomainSongCollection
											)
										},
										onAddToQueue = {
											if (selectedAlbum != null) {
												player.addToQueue(
													selectedAlbum as DomainSongCollection
												)
											}
										},
										rating = selectedAlbum?.userRating ?: 0,
										onSetRating = { it: Int -> viewModel.setRating(it) }
									)
								} else {
									AlbumListScreenListItem(
										modifier = Modifier.animateItem(),
										album = album,
										selected = album == selectedAlbum,
										starred = selectedAlbum?.starredAt != null,
										onSelect = {
											viewModel.selected = album
										},
										onDeselect = {
											viewModel.selected = null
										},
										onSetStarred = {
											viewModel.starAlbum(it)
										},
										onSetShareId = { newShareId: String ->
											shareId = newShareId
										},
										onPlayNext = {
											if (selectedAlbum != null) player.playNext(
												selectedAlbum as DomainSongCollection
											)
										},
										onAddToQueue = {
											if (selectedAlbum != null) {
												player.addToQueue(
													selectedAlbum as DomainSongCollection
												)
											}
										},
										rating = selectedAlbum?.userRating ?: 0,
										onSetRating = { it: Int -> viewModel.setRating(it) }
									)
								}
							}
						}
					} else if (isLoading) {
						artGridPlaceholder(viewMode = selectedViewMode)
					} else {
						item(span = { GridItemSpan(maxLineSpan) }) {
							ContentUnavailable(label = stringResource(info_no_albums))
						}
					}
				}
				if (selectedSorting == DomainAlbumListType.AlphabeticalByName) {
					AlphabeticalScroller(
						state = viewModel.gridState,
						headers = headerIndices,
						modifier = Modifier.align(Alignment.TopEnd)
					)
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
}
