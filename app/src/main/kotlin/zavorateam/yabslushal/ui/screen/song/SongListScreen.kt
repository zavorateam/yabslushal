/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.song

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zavorateam.yabslushal.R
import zavorateam.yabslushal.R.string.info_no_songs
import zavorateam.yabslushal.di.LocalBottomBarScrollManager
import zavorateam.yabslushal.di.LocalSizeClass
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.manager.session.DownloadManager
import zavorateam.yabslushal.domain.model.DomainSong
import zavorateam.yabslushal.domain.model.DomainSongListType
import zavorateam.yabslushal.domain.model.settings.BottomBarVisibilityMode
import zavorateam.yabslushal.domain.model.settings.ListViewMode
import zavorateam.yabslushal.playback.MediaPlayer
import zavorateam.yabslushal.ui.component.common.ContentUnavailable
import zavorateam.yabslushal.ui.component.dialog.DeleteDownloadTarget
import zavorateam.yabslushal.ui.component.dialog.QueueDuplicateDialog
import zavorateam.yabslushal.ui.component.dialog.ShareDialog
import zavorateam.yabslushal.ui.component.dialog.rememberDownloadDeletionHandler
import zavorateam.yabslushal.ui.component.layout.ArtGridPlaceholder
import zavorateam.yabslushal.ui.component.layout.NestedTopBar
import zavorateam.yabslushal.ui.component.layout.PullToRefreshBox
import zavorateam.yabslushal.ui.component.layout.RootBottomBar
import zavorateam.yabslushal.ui.component.layout.RootTopBar
import zavorateam.yabslushal.ui.core.UiState
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.filled.Play
import zavorateam.yabslushal.ui.icons.outlined.Shuffle
import zavorateam.yabslushal.ui.navigation.PersistentViewModelStoreOwner
import zavorateam.yabslushal.ui.screen.song.component.SongListScreenItem
import zavorateam.yabslushal.ui.screen.song.component.SongListScreenSortButton
import zavorateam.yabslushal.ui.screen.song.viewmodel.SongListViewModel
import zavorateam.yabslushal.ui.util.withoutTop
import zavorateam.yabslushal.ui.viewmodel.RootViewModel

@Composable
fun SongListScreen(
	nested: Boolean,
	listType: DomainSongListType? = null,
	starredOnly: Boolean = false
) {
	val initialFilters = if (starredOnly) setOf(zavorateam.yabslushal.domain.model.DomainFilter.Starred) else null
	val viewModel = koinViewModel<SongListViewModel>(
		key = "${listType}_starred_$starredOnly",
		parameters = { parametersOf(listType, initialFilters) },
		viewModelStoreOwner = if (nested) {
			LocalViewModelStoreOwner.current!!
		} else {
			koinInject<PersistentViewModelStoreOwner>()
		}
	)
	val preferenceManager = koinInject<PreferenceManager>()
	val player = koinInject<MediaPlayer>()
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val lazyPagingItems = state.data?.items?.collectAsLazyPagingItems()
	val downloadManager = koinInject<DownloadManager>()

	val selectedSorting by viewModel.selectedSorting.collectAsStateWithLifecycle()
	val selectedReversed by viewModel.selectedReversed.collectAsStateWithLifecycle()
	val selectedFilters by viewModel.selectedFilters.collectAsStateWithLifecycle()
	val allDownloads by downloadManager.allDownloads.collectAsStateWithLifecycle(
		initialValue = emptyList()
	)

	var shareId by remember { mutableStateOf<String?>(null) }
	var songToQueue by remember { mutableStateOf<DomainSong?>(null) }
	val downloadDeletionHandler = rememberDownloadDeletionHandler()
	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

	val actions: @Composable RowScope.() -> Unit = {
		SongListScreenSortButton(
			nested = nested,
			selectedSorting = selectedSorting,
			onSetSorting = { viewModel.setSorting(it) },
			selectedReversed = selectedReversed,
			onSetReversed = { viewModel.setReversed(it) },
			selectedFilters = selectedFilters,
			onToggleFilter = { viewModel.toggleFilter(it) }
		)
	}

	val rootViewModel = koinViewModel<RootViewModel>()
	val listState = rememberLazyListState()
	LaunchedEffect(Unit) {
		rootViewModel.events.collect { event ->
			if (event is RootViewModel.Event.ScrollToTop) {
				listState.animateScrollToItem(0)
			}
		}
	}

	val downloadsMap = remember(allDownloads) { allDownloads.associateBy { it.songId } }

	Scaffold(
		topBar = {
			if (!nested) {
				RootTopBar(
					title = { Text(stringResource(R.string.title_songs)) },
					scrollBehavior = scrollBehavior,
					actions = actions
				)
			} else {
				NestedTopBar(
					title = { Text(stringResource(R.string.title_songs)) },
					actions = actions
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
			onRefresh = { viewModel.refreshSongs(true) },
			key = state
		) {
			LazyColumn(
				modifier = if (!nested)
					Modifier
						.fillMaxSize()
						.nestedScroll(scrollBehavior.nestedScrollConnection)
				else Modifier.fillMaxSize(),
				contentPadding = innerPadding.withoutTop(),
				verticalArrangement = if (itemCount == 0 && !isLoading)
					Arrangement.Center
				else Arrangement.spacedBy(12.dp),
				state = listState
			) {
				if (itemCount > 0 && lazyPagingItems != null) {
					item {
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(horizontal = 16.dp, vertical = 6.dp),
							horizontalArrangement = Arrangement.spacedBy(10.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							OutlinedButton(
								modifier = Modifier.size(width = 52.dp, height = 44.dp),
								onClick = {
									val items = lazyPagingItems.itemSnapshotList.items.filterNotNull()
									if (items.isNotEmpty()) {
										player.shufflePlay(items)
									}
								},
								shape = CircleShape,
								contentPadding = PaddingValues(0.dp)
							) {
								Icon(
									Icons.Outlined.Shuffle,
									contentDescription = stringResource(R.string.action_shuffle),
									modifier = Modifier.size(24.dp)
								)
							}
							Button(
								modifier = Modifier
									.weight(1f)
									.height(44.dp),
								onClick = {
									val items = lazyPagingItems.itemSnapshotList.items.filterNotNull()
									if (items.isNotEmpty()) {
										player.playNow(items, 0)
									}
								},
								shape = CircleShape
							) {
								Icon(
									Icons.Filled.Play,
									null,
									modifier = Modifier
										.size(25.dp)
										.padding(end = 3.dp)
								)
								Text(
									stringResource(R.string.action_play),
									fontWeight = FontWeight.SemiBold
								)
							}
						}
					}
					items(itemCount) { index ->
						val song = lazyPagingItems[index] ?: return@items

						val download = downloadsMap[song.id]
						SongListScreenItem(
							modifier = Modifier.animateItem(),
							song = song,
							selected = song == state.data?.selected,
							starred = song.starredAt != null,
							rating = song.userRating ?: 0,
							onSelect = {
								viewModel.selected = song
							},
							onDeselect = {
								viewModel.clear()
							},
							onSetStarred = {
								viewModel.starSong(it)
							},
							onSetShareId = {
								shareId = it
							},
							onPlayNext = {
								if (player.checkDuplicate(song.id, preferenceManager)) {
									songToQueue = song
								} else {
									player.playNextSingle(song)
								}
							},
							onAddToQueue = {
								if (player.checkDuplicate(song.id, preferenceManager)) {
									songToQueue = song
								} else {
									player.addToQueueSingle(song)
								}
							},
							onClick = {
								val items = lazyPagingItems?.itemSnapshotList?.items?.filterNotNull().orEmpty()
								if (items.isNotEmpty()) {
									val songIndex = items.indexOf(song).coerceAtLeast(0)
									player.playNow(items, songIndex)
								} else {
									player.playNow(song)
								}
							},
							onSetRating = { it: Int -> viewModel.rateSelectedSong(it) },
							download = download,
							onDownload = {
								downloadManager.downloadSong(song.id)
							},
							onCancelDownload = {
								downloadManager.cancelDownload(song.id)
							},
							onDeleteDownload = {
								downloadDeletionHandler.requestDelete(DeleteDownloadTarget.Single(song.id, song.title))
							}
						)
					}
				} else if (isLoading) {
					items(10) {
						ArtGridPlaceholder(viewMode = ListViewMode.List)
					}
				} else {
					item {
						ContentUnavailable(
							label = stringResource(info_no_songs)
						)
					}
				}
			}
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
