/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.song.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.persistentListOf
import zavorateam.yabslushal.domain.model.DomainFilter
import zavorateam.yabslushal.domain.model.DomainSongListType
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.outlined.Sort
import zavorateam.yabslushal.ui.component.layout.TopBarButton
import zavorateam.yabslushal.ui.component.sheet.SortSheet
import zavorateam.yabslushal.ui.util.label

@Composable
fun SongListScreenSortButton(
	nested: Boolean,
	selectedSorting: DomainSongListType,
	onSetSorting: (listType: DomainSongListType) -> Unit,
	selectedReversed: Boolean,
	onSetReversed: (Boolean) -> Unit,
	selectedFilters: Set<DomainFilter>,
	onToggleFilter: (DomainFilter) -> Unit
) {
	val entries = remember {
		persistentListOf(
			DomainSongListType.FrequentlyPlayed,
			DomainSongListType.Newest,
			DomainSongListType.Random,
			DomainSongListType.Rating,
			DomainSongListType.Year
		)
	}
	var expanded by remember { mutableStateOf(false) }
	if (!nested) {
		IconButton(onClick = {
			expanded = true
		}) {
			Icon(
				imageVector = Icons.Outlined.Sort,
				contentDescription = null
			)
		}
	} else {
		TopBarButton(onClick = { expanded = true }) {
			Icon(
				imageVector = Icons.Outlined.Sort,
				contentDescription = null
			)
		}
	}
	if (expanded) {
		SortSheet(
			entries = entries,
			onDismissRequest = { expanded = false },
			selectedSorting = selectedSorting,
			onSetSorting = onSetSorting,
			selectedReversed = selectedReversed,
			label = { it.label() },
			onSetReversed = onSetReversed,
			selectedFilters = selectedFilters,
			onToggleFilter = onToggleFilter
		)
	}
}
