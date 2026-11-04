/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import zavorateam.yabslushal.database.dao.PlaylistDao
import zavorateam.yabslushal.database.mapper.toDomainModel
import zavorateam.yabslushal.database.paging.createPagedFlow
import zavorateam.yabslushal.database.query.toSqlQuery
import zavorateam.yabslushal.domain.model.DomainFilter
import zavorateam.yabslushal.domain.model.DomainPlaylist
import zavorateam.yabslushal.domain.model.DomainPlaylistListType

class PlaylistRepository(
	private val playlistDao: PlaylistDao,
	private val dbRepository: DbRepository,
) {
	private suspend fun getLocalData(
		listType: DomainPlaylistListType,
		reversed: Boolean,
		filters: Set<DomainFilter> = emptySet()
	): Flow<PagingData<DomainPlaylist>> = createPagedFlow(
		isRandom = (listType == DomainPlaylistListType.Random),
		getRandomIds = { playlistDao.getPlaylistIdsByQuery(listType.toSqlQuery(filters = filters, idsOnly = true)) },
		getEntityId = { it.playlist.playlistId },
		fetchByIds = playlistDao::getPlaylistsByIds,
		getStandardQuery = { playlistDao.getPlaylistsByQuery(listType.toSqlQuery(reversed, filters)) },
		toDomain = { it.toDomainModel() }
	)

	private suspend fun refreshLocalData(
		listType: DomainPlaylistListType,
		reversed: Boolean,
		filters: Set<DomainFilter> = emptySet()
	): Flow<PagingData<DomainPlaylist>> {
		dbRepository.syncPlaylists().getOrThrow().forEach { playlist ->
			dbRepository.syncPlaylistSongs(playlist.playlistId).getOrThrow()
		}
		return getLocalData(listType, reversed, filters)
	}

	suspend fun getPlaylists(
		fullRefresh: Boolean,
		listType: DomainPlaylistListType,
		reversed: Boolean,
		filters: Set<DomainFilter> = emptySet()
	): Flow<PagingData<DomainPlaylist>> {
		val localData = getLocalData(listType, reversed, filters)
		return if (fullRefresh) {
			refreshLocalData(listType, reversed, filters)
		} else {
			localData
		}
	}
}
