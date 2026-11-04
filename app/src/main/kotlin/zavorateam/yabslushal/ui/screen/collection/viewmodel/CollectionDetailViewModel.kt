/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.collection.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import zavorateam.yabslushal.R
import zavorateam.yabslushal.database.mapper.toDomainModel
import zavorateam.yabslushal.domain.manager.session.connectivity.ReachabilityFlow
import zavorateam.yabslushal.domain.manager.session.SessionManager
import zavorateam.yabslushal.domain.manager.session.SessionState
import zavorateam.yabslushal.domain.manager.ui.SnackBarManager
import zavorateam.yabslushal.domain.model.DomainAlbum
import zavorateam.yabslushal.domain.model.DomainAlbumInfo
import zavorateam.yabslushal.domain.model.DomainSong
import zavorateam.yabslushal.domain.model.DomainSongCollection
import zavorateam.yabslushal.domain.repository.AlbumRepository
import zavorateam.yabslushal.domain.repository.CollectionRepository
import zavorateam.yabslushal.domain.repository.SongRepository
import zavorateam.yabslushal.ui.viewmodel.AsyncViewModel

data class CollectionViewState(
	val collection: DomainSongCollection? = null,
	val starred: Boolean = false,
	val rating: Int = 0,
	val albumInfo: DomainAlbumInfo? = null,
	val selectedSong: DomainSong? = null,
	val selectedIndex: Int? = null,
	val selectedAlbum: DomainAlbum? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionDetailViewModel(
	private val collectionId: String,
	private val repository: CollectionRepository,
	private val songRepository: SongRepository,
	private val albumRepository: AlbumRepository,
	private val sessionManager: SessionManager,
	private val snackBarManager: SnackBarManager,
	reachability: ReachabilityFlow
) : AsyncViewModel<CollectionViewState>() {

	init {
		execute {
			CollectionViewState(
				collection = repository.getLocalData(collectionId)
			)
		}
	}

	val listState = LazyListState()

	val isReachable = reachability.isReachable

	val pagedSongs: Flow<PagingData<DomainSong>> = repository
		.getPagedPlaylistSongs(collectionId)
		.cachedIn(viewModelScope)

	val otherAlbums = uiState
		.map { state -> (state.data?.collection as? DomainAlbum) }
		.flatMapLatest { album ->
			album?.let { repository.getOtherAlbums(it.artistId, it.id) } ?: emptyFlow()
		}
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Lazily,
			initialValue = emptyList()
		)

	init {
		launch {
			sessionManager.sessionState.collect { if (it is SessionState.Authenticated) refreshCollection(false) }
		}
	}

	fun refreshCollection(fullRefresh: Boolean) = launch {
		val collection = repository.getCollection(fullRefresh, collectionId)
		val state = if (collection is DomainAlbum) {
			CollectionViewState(
				collection = collection,
				starred = albumRepository.isAlbumStarred(collection.id),
				rating = albumRepository.getAlbumRating(collection.id),
				albumInfo = repository.getAlbumInfo(collectionId).toDomainModel()
			)
		} else {
			CollectionViewState(collection = collection)
		}
		setSuccess(state)
	}

	fun selectSong(song: DomainSong, index: Int) = updateData {
		it.copy(
			selectedSong = song,
			selectedIndex = index
		)
	}

	fun selectAlbum(album: DomainAlbum) = updateData {
		it.copy(
			selectedAlbum = album
		)
	}

	fun clearSelection() = updateData {
		it.copy(selectedSong = null, selectedAlbum = null)
	}

	fun removeFromPlaylist() {
		val index = uiState.value.data?.selectedIndex ?: return
		launch {
			repository.removeSongFromPlaylist(collectionId, index)
			snackBarManager.notify(R.string.notice_removed_from_playlist)
			refreshCollection(true)
		}
		clearSelection()
	}

	fun starSelectedSong() = launch {
		val selection = uiState.value.data?.selectedSong ?: return@launch
		songRepository.starSong(selection.id)
		refreshCollection(false)
	}

	fun unstarSelectedSong() = launch {
		val selection = uiState.value.data?.selectedSong ?: return@launch
		songRepository.unstarSong(selection.id)
		refreshCollection(false)
	}

	fun rateSelectedSong(rating: Int) = launch {
		val selection = uiState.value.data?.selectedSong ?: return@launch
		songRepository.rateSong(selection.id, rating)
	}

	fun rateAlbum(newRating: Int) = launch {
		(uiState.value.data?.collection as? DomainAlbum)?.let { album ->
			albumRepository.rateAlbum(album.id, newRating)
		}
	}

	fun starAlbum(starred: Boolean) {
		launch {
			val collection = data?.collection
			if (collection !is DomainAlbum) return@launch
			if (starred) {
				albumRepository.starAlbum(collection.id)
			} else {
				albumRepository.unstarAlbum(collection.id)
			}
			refreshCollection(false)
		}
	}

	fun rateSelectedAlbum(rating: Int) {
		launch {
			data?.selectedAlbum?.let { albumRepository.rateAlbum(it.id, rating) }
		}
	}

	fun starSelectedAlbum(starred: Boolean) {
		launch {
			val album = data?.selectedAlbum ?: return@launch
			if (starred) {
				albumRepository.starAlbum(album.id)
			} else {
				albumRepository.unstarAlbum(album.id)
			}
		}
	}

}
