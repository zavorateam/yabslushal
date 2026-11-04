/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.artist.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import zavorateam.yabslushal.domain.model.DomainArtistListType
import zavorateam.yabslushal.domain.model.DomainFilter
import zavorateam.yabslushal.domain.model.settings.ListViewMode
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.outlined.Sort
import zavorateam.yabslushal.ui.component.layout.TopBarButton
import zavorateam.yabslushal.ui.component.sheet.SortSheet

@Composable
fun ArtistListScreenSortButton(
	nested: Boolean,
	selectedSorting: DomainArtistListType,
	onSetSorting: (DomainArtistListType) -> Unit,
	selectedViewMode: ListViewMode,
	onSetViewMode: (ListViewMode) -> Unit,
	selectedFilters: Set<DomainFilter>,
	onToggleFilter: (DomainFilter) -> Unit
) {
	val entries = remember {
		persistentListOf(
			DomainArtistListType.AlphabeticalByName,
			DomainArtistListType.Random
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
			selectedSorting = selectedSorting,
			label = { stringResource(it.displayName) },
			onSetSorting = onSetSorting,
			onDismissRequest = { expanded = false },
			selectedViewMode = selectedViewMode,
			onSetViewMode = onSetViewMode,
			selectedFilters = selectedFilters,
			onToggleFilter = onToggleFilter
		)
	}
}
