/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import zavorateam.yabslushal.R.string.info_no_radios
import zavorateam.yabslushal.di.LocalBottomBarScrollManager
import zavorateam.yabslushal.di.LocalSizeClass
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.model.settings.BottomBarCollapseMode
import zavorateam.yabslushal.domain.model.settings.BottomBarVisibilityMode
import zavorateam.yabslushal.playback.MediaPlayer
import zavorateam.yabslushal.ui.component.common.ContentUnavailable
import zavorateam.yabslushal.ui.component.dialog.DeletionDialog
import zavorateam.yabslushal.ui.component.dialog.DeletionTarget
import zavorateam.yabslushal.ui.component.layout.ArtGrid
import zavorateam.yabslushal.ui.component.layout.NestedTopBar
import zavorateam.yabslushal.ui.component.layout.PullToRefreshBox
import zavorateam.yabslushal.ui.component.layout.RootBottomBar
import zavorateam.yabslushal.ui.component.layout.RootTopBar
import zavorateam.yabslushal.ui.component.sheet.RadioSheet
import zavorateam.yabslushal.ui.component.snackbar.ErrorSnackBar
import zavorateam.yabslushal.ui.core.UiState
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.outlined.Add
import zavorateam.yabslushal.ui.navigation.PersistentViewModelStoreOwner
import zavorateam.yabslushal.ui.screen.radio.component.RadioListScreenCard
import zavorateam.yabslushal.ui.screen.radio.component.RadioListScreenCardPlaceholder
import zavorateam.yabslushal.ui.screen.radio.dialog.RadioCreateDialog
import zavorateam.yabslushal.ui.screen.radio.dialog.RadioUpdateDialog
import zavorateam.yabslushal.ui.screen.radio.viewmodel.RadioListViewModel
import zavorateam.yabslushal.ui.util.withoutTop
import zavorateam.yabslushal.ui.viewmodel.RootViewModel

@Composable
fun RadioListScreen(
	nested: Boolean
) {
	val scrollManager = LocalBottomBarScrollManager.current
	val viewModel = koinViewModel<RadioListViewModel>(
		viewModelStoreOwner = if (nested) {
			LocalViewModelStoreOwner.current!!
		} else {
			koinInject<PersistentViewModelStoreOwner>()
		}
	)
	val player = koinInject<MediaPlayer>()
	val state by viewModel.uiState.collectAsStateWithLifecycle()
	val lazyPagingItems = state.data?.items?.collectAsLazyPagingItems()

	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
	var createDialogShown by rememberSaveable { mutableStateOf(false) }
	var updateDialogShown by rememberSaveable { mutableStateOf(false) }
	var deleteDialogShown by rememberSaveable { mutableStateOf(false) }
	val preferenceManager = koinInject<PreferenceManager>()

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
					{ Text(stringResource(R.string.title_radios)) },
					scrollBehavior
				)
			} else {
				NestedTopBar({ Text(stringResource(R.string.title_radios)) })
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
					contentDescription = stringResource(R.string.title_create_radio),
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
			onRefresh = { viewModel.refreshRadios(true) },
			key = state
		) {
			ArtGrid(
				modifier = if (!nested)
					Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
				else Modifier,
				contentPadding = innerPadding.withoutTop(),
				state = viewModel.gridState,
				verticalArrangement = if (itemCount == 0 && !isLoading)
					Arrangement.Center
				else Arrangement.spacedBy(12.dp)
			) {
				if (itemCount > 0 && lazyPagingItems != null) {
					items(itemCount) { index ->
						val radio = lazyPagingItems[index]
						if (radio != null) {
							RadioListScreenCard(
								modifier = Modifier.animateItem(),
								radio = radio,
								onClick = { player.playRadio(radio) },
								onLongClick = { viewModel.selected = radio }
							)
						}
					}
				} else if (isLoading) {
					items(10) {
						RadioListScreenCardPlaceholder()
					}
				} else {
					item(span = { GridItemSpan(maxLineSpan) }) {
						ContentUnavailable(
							label = stringResource(info_no_radios)
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

	if (createDialogShown) {
		RadioCreateDialog(
			onDismissRequest = { createDialogShown = false },
			onRefresh = { viewModel.refreshRadios(true) }
		)
	}

	viewModel.selected?.let { selectedRadio ->
		RadioSheet(
			onDismissRequest = { viewModel.selected = null },
			radio = selectedRadio,
			onUpdateRadioRequest = { updateDialogShown = true },
			onDeleteRadioRequest = { deleteDialogShown = true }
		)

		if (updateDialogShown) {
			RadioUpdateDialog(
				onDismissRequest = { updateDialogShown = false },
				radio = selectedRadio,
				onRefresh = {
					updateDialogShown = false
					viewModel.selected = null
					viewModel.refreshRadios(true)
				}
			)
		}

		DeletionDialog(
			target = if (deleteDialogShown) DeletionTarget.Radio(selectedRadio.id) else null,
			onDismissRequest = {
				deleteDialogShown = false
				viewModel.selected = null
			},
			onRefresh = { viewModel.refreshRadios(true) }
		)
	}
}
