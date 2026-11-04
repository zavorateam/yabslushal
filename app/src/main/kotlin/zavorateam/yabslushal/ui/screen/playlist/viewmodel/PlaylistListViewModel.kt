/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.playlist.viewmodel

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.manager.session.SessionManager
import zavorateam.yabslushal.domain.manager.session.SessionState
import zavorateam.yabslushal.domain.model.DomainFilter
import zavorateam.yabslushal.domain.model.DomainPlaylist
import zavorateam.yabslushal.domain.model.DomainPlaylistListType
import zavorateam.yabslushal.domain.model.toBitmask
import zavorateam.yabslushal.domain.model.toDomainFilters
import zavorateam.yabslushal.domain.repository.PlaylistRepository
import zavorateam.yabslushal.ui.viewmodel.SelectableViewModel


class PlaylistListViewModel(
	private val repository: PlaylistRepository,
	private val sessionManager: SessionManager,
	private val preferenceManager: PreferenceManager
) : SelectableViewModel<DomainPlaylist>() {

	val selectedSorting: StateFlow<DomainPlaylistListType>
		field = MutableStateFlow(preferenceManager.playlistSortType)

	val selectedReversed: StateFlow<Boolean>
		field = MutableStateFlow(preferenceManager.playlistSortReversed)

	val selectedFilters: StateFlow<Set<DomainFilter>>
		field = MutableStateFlow(preferenceManager.playlistFilters.toDomainFilters())

	val gridState = LazyGridState()

	init {
		viewModelScope.launch {
			sessionManager.sessionState.collect { if (it is SessionState.Authenticated) refreshPlaylists(false) }
		}
	}

	fun refreshPlaylists(fullRefresh: Boolean) = launch {
		items = repository.getPlaylists(
			fullRefresh,
			selectedSorting.value,
			selectedReversed.value,
			selectedFilters.value
		).cachedIn(viewModelScope)
	}

	fun setSorting(sorting: DomainPlaylistListType) {
		selectedSorting.value = sorting
		preferenceManager.playlistSortType = sorting
		refreshPlaylists(false)
	}

	fun setReversed(reversed: Boolean) {
		selectedReversed.value = reversed
		preferenceManager.playlistSortReversed = reversed
		refreshPlaylists(false)
	}

	fun toggleFilter(filter: DomainFilter) {
		val current = selectedFilters.value
		val newFilters = if (current.contains(filter)) {
			current - filter
		} else {
			current + filter
		}
		selectedFilters.value = newFilters
		preferenceManager.playlistFilters = newFilters.toBitmask()
		refreshPlaylists(false)
	}

}
