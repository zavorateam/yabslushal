/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import org.koin.compose.viewmodel.koinViewModel
import zavorateam.yabslushal.R
import zavorateam.yabslushal.di.LocalSizeClass
import zavorateam.yabslushal.domain.model.settings.LibrarySection
import zavorateam.yabslushal.ui.component.common.SegmentedListItem
import zavorateam.yabslushal.ui.component.common.SegmentedListItemDefaults
import zavorateam.yabslushal.ui.component.layout.NestedTopBar
import zavorateam.yabslushal.ui.component.layout.NestedTopBarDefaults
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.outlined.DragHandle
import zavorateam.yabslushal.ui.screen.settings.component.SettingsGroup
import zavorateam.yabslushal.ui.screen.settings.component.SettingsGroupDefaults
import zavorateam.yabslushal.ui.screen.settings.component.SettingsNavItem
import zavorateam.yabslushal.ui.screen.settings.viewmodel.LibraryLayoutViewModel
import zavorateam.yabslushal.ui.util.dragHandle
import zavorateam.yabslushal.ui.util.draggableItems
import zavorateam.yabslushal.ui.util.rememberDraggableListState

@Composable
fun SettingsLibraryLayoutScreen() {
	val sizeClass = LocalSizeClass.current
	val hideBack = sizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
	val viewModel = koinViewModel<LibraryLayoutViewModel>()
	val haptic = LocalHapticFeedback.current

	val draggableState = rememberDraggableListState { from, to ->
		viewModel.move(from, to)
		haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
	}

	val config = viewModel.config
	val visibleCount = config.sections.count { it.visible }

	Scaffold(
		topBar = {
			NestedTopBar(
				title = { Text(stringResource(R.string.title_library_sections)) },
				navigationAction = {
					if (!hideBack) {
						NestedTopBarDefaults.NavigationAction()
					}
				}
			)
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.padding(innerPadding)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 16.dp),
			verticalArrangement = Arrangement.spacedBy(SettingsGroupDefaults.GapBetweenGroups)
		) {
			LazyColumn(
				modifier = Modifier
					.fillMaxWidth()
					.heightIn(max = 480.dp),
				state = draggableState.listState,
				verticalArrangement = Arrangement.spacedBy(SettingsGroupDefaults.GapBetweenGroups)
			) {
				draggableItems(
					state = draggableState,
					items = config.sections,
					key = { section -> section.id }
				) { section, isDragging ->
					LibrarySectionRow(
						section = section,
						isDragging = isDragging,
						isOnlyVisible = section.visible && visibleCount == 1,
						onToggleVisibility = { viewModel.toggleVisibility(section.id) },
						onDragHandle = {
							Modifier.dragHandle(
								state = draggableState,
								key = section.id
							)
						}
					)
				}
			}

			SettingsGroup {
				SettingsNavItem(
					onClick = dropUnlessResumed { viewModel.reset() },
					content = { Text(stringResource(R.string.title_reset_order)) },
					supportingContent = {
						Text(stringResource(R.string.subtitle_reset_library_sections))
					},
					shapes = SegmentedListItemDefaults.segmentedShapes(index = 0, count = 1)
				)
			}
		}
	}
}

@Composable
private fun LibrarySectionRow(
	section: LibrarySection,
	isDragging: Boolean,
	isOnlyVisible: Boolean,
	onToggleVisibility: () -> Unit,
	onDragHandle: () -> Modifier
) {
	val elevation by animateDpAsState(
		if (isDragging) 4.dp else 0.dp,
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
	)

	Surface(
		shadowElevation = elevation,
		modifier = Modifier.fillMaxWidth(),
		shape = MaterialTheme.shapes.large
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Checkbox(
				enabled = !isOnlyVisible,
				checked = section.visible,
				onCheckedChange = { _ -> onToggleVisibility() }
			)
			Text(
				text = stringResource(section.id.title),
				modifier = Modifier.weight(1f),
				style = MaterialTheme.typography.bodyLarge
			)
			IconButton(
				modifier = onDragHandle(),
				onClick = {}
			) {
				Icon(
					Icons.Outlined.DragHandle,
					contentDescription = stringResource(R.string.action_reorder)
				)
			}
		}
	}
}

/** Localized title of a library section. */
val LibrarySection.Id.title: Int
	get() = when (this) {
		LibrarySection.Id.MIX_IT_UP -> R.string.title_section_mix_it_up
		LibrarySection.Id.RECENT_ALBUMS -> R.string.option_sort_recent
		LibrarySection.Id.PLAYLISTS -> R.string.title_playlists
		LibrarySection.Id.ARTISTS -> R.string.title_artists
		LibrarySection.Id.GENRES -> R.string.title_genres
		LibrarySection.Id.STATISTICS -> R.string.title_statistics
	}
