/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.artist.viewmodel

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import zavorateam.yabslushal.database.dao.AlbumDao
import zavorateam.yabslushal.database.dao.LetterCount
import zavorateam.yabslushal.database.mapper.toDomainModel
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.manager.session.SessionManager
import zavorateam.yabslushal.domain.manager.session.SessionState
import zavorateam.yabslushal.domain.model.DomainArtist
import zavorateam.yabslushal.domain.model.DomainArtistListType
import zavorateam.yabslushal.domain.model.DomainFilter
import zavorateam.yabslushal.domain.model.toBitmask
import zavorateam.yabslushal.domain.model.toDomainFilters
import zavorateam.yabslushal.domain.repository.ArtistRepository
import zavorateam.yabslushal.playback.MediaPlayer
import zavorateam.yabslushal.ui.viewmodel.SelectableViewModel


class ArtistListViewModel(
	initialListType: DomainArtistListType? = null,
	initialFilters: Set<DomainFilter>? = null,
	private val repository: ArtistRepository,
	private val albumDao: AlbumDao,
	private val sessionManager: SessionManager,
	private val preferenceManager: PreferenceManager
) : SelectableViewModel<DomainArtist>() {

	val listType: StateFlow<DomainArtistListType>
		field = MutableStateFlow(initialListType ?: preferenceManager.artistSortType)

	val selectedFilters: StateFlow<Set<DomainFilter>>
		field = MutableStateFlow(
			initialFilters ?: preferenceManager.artistFilters.toDomainFilters()
		)

	val letterCounts: StateFlow<List<LetterCount>>
		field = MutableStateFlow(emptyList())

	val gridState = LazyGridState()

	private var letterCountsJob: Job? = null

	init {
		launch {
			sessionManager.sessionState.collect { if (it is SessionState.Authenticated) refreshArtists(false) }
		}
	}

	fun refreshArtists(fullRefresh: Boolean) = launch {
		items = repository.getArtists(
			fullRefresh,
			listType.value,
			selectedFilters.value
		).cachedIn(viewModelScope)
		letterCountsJob?.cancel()
		letterCountsJob = viewModelScope.launch {
			repository.getArtistLetterCounts(selectedFilters.value).collect {
				letterCounts.value = it
			}
		}
	}

	fun selectArtist(artist: DomainArtist?) {
		selected = artist
	}

	fun clearSelection() = selectArtist(null)

	fun starArtist(isStarred: Boolean) {
		launch {
			val artist = selected ?: return@launch
			if (isStarred) repository.starArtist(artist.id) else repository.unstarArtist(artist.id)
			refreshArtists(false)
		}
	}

	fun addArtistAlbumsToQueue(player: MediaPlayer) {
		val artist = selected ?: return
		viewModelScope.launch {
			val artistAlbums =
				albumDao.getAlbumsByArtist(artist.id).firstOrNull() ?: emptyList()
			artistAlbums.map { it.toDomainModel() }.forEach { album ->
				player.addToQueue(album)
			}
		}
	}

	fun playArtistAlbumsNext(player: MediaPlayer) {
		val artist = selected ?: return
		launch {
			val artistAlbums =
				albumDao.getAlbumsByArtist(artist.id).firstOrNull() ?: emptyList()
			artistAlbums.map { it.toDomainModel() }.forEach { album ->
				player.playNext(album)
			}
		}
	}

	fun setListType(newListType: DomainArtistListType) {
		listType.value = newListType
		preferenceManager.artistSortType = newListType
		refreshArtists(false)
	}

	fun toggleFilter(filter: DomainFilter) {
		val current = selectedFilters.value
		val newFilters = if (current.contains(filter)) {
			current - filter
		} else {
			current + filter
		}
		selectedFilters.value = newFilters
		preferenceManager.artistFilters = newFilters.toBitmask()
		refreshArtists(false)
	}

}
