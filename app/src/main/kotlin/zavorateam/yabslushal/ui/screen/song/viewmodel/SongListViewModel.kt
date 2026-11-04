/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.song.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.MutableStateFlow
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.manager.session.connectivity.ReachabilityFlow
import zavorateam.yabslushal.domain.manager.session.SessionManager
import zavorateam.yabslushal.domain.manager.session.SessionState
import zavorateam.yabslushal.domain.model.DomainFilter
import zavorateam.yabslushal.domain.model.DomainSong
import zavorateam.yabslushal.domain.model.DomainSongListType
import zavorateam.yabslushal.domain.model.toBitmask
import zavorateam.yabslushal.domain.model.toDomainFilters
import zavorateam.yabslushal.domain.repository.SongRepository
import zavorateam.yabslushal.ui.viewmodel.SelectableViewModel


class SongListViewModel(
	initialListType: DomainSongListType? = null,
	initialFilters: Set<DomainFilter>? = null,
	private val repository: SongRepository,
	private val sessionManager: SessionManager,
	private val preferenceManager: PreferenceManager,
	reachability: ReachabilityFlow
) : SelectableViewModel<DomainSong>() {

	val selectedSorting = MutableStateFlow(initialListType ?: preferenceManager.songSortType)

	val selectedReversed = MutableStateFlow(preferenceManager.songSortReversed)

	val selectedFilters = MutableStateFlow(
		initialFilters ?: preferenceManager.songFilters.toDomainFilters()
	)

	val isReachable = reachability.isReachable

	init {
		launch {
			sessionManager.sessionState.collect { if (it is SessionState.Authenticated) refreshSongs(false) }
		}
	}

	fun refreshSongs(fullRefresh: Boolean) = launch {
		items = repository.getSongs(
			fullRefresh,
			selectedSorting.value,
			selectedReversed.value,
			selectedFilters.value
		).cachedIn(viewModelScope)
	}

	fun starSong(isStarred: Boolean) {
		launch {
			val selection = selected ?: return@launch
			if (isStarred) {
				repository.starSong(selection.id)
			} else {
				repository.unstarSong(selection.id)
			}
			refreshSongs(false)
		}
	}

	fun rateSelectedSong(rating: Int) {
		launch {
			val selection = selected ?: return@launch
			repository.rateSong(selection.id, rating)
		}
	}

	fun setSorting(sorting: DomainSongListType) {
		selectedSorting.value = sorting
		preferenceManager.songSortType = sorting
		refreshSongs(false)
	}

	fun setReversed(reversed: Boolean) {
		selectedReversed.value = reversed
		preferenceManager.songSortReversed = reversed
		refreshSongs(false)
	}

	fun toggleFilter(filter: DomainFilter) {
		val current = selectedFilters.value
		val newFilters = if (current.contains(filter)) {
			current - filter
		} else {
			current + filter
		}
		selectedFilters.value = newFilters
		preferenceManager.songFilters = newFilters.toBitmask()
		refreshSongs(false)
	}
}
