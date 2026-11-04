/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import zavorateam.yabslushal.database.dao.ArtistDao
import zavorateam.yabslushal.database.dao.LetterCount
import zavorateam.yabslushal.database.entity.SyncActionType
import zavorateam.yabslushal.database.mapper.toDomainModel
import zavorateam.yabslushal.database.paging.createPagedFlow
import zavorateam.yabslushal.database.query.getArtistLetterCountsQuery
import zavorateam.yabslushal.database.query.toSqlQuery
import zavorateam.yabslushal.domain.manager.session.SyncManager
import zavorateam.yabslushal.domain.model.DomainArtist
import zavorateam.yabslushal.domain.model.DomainArtistListType
import zavorateam.yabslushal.domain.model.DomainFilter
import kotlin.time.Clock

class ArtistRepository(
	private val artistDao: ArtistDao,
	private val syncManager: SyncManager,
	private val dbRepository: DbRepository
) {
	private suspend fun getLocalData(
		listType: DomainArtistListType,
		filters: Set<DomainFilter> = emptySet(),
		reversed: Boolean = false,
	): Flow<PagingData<DomainArtist>> = createPagedFlow(
		isRandom = (listType == DomainArtistListType.Random),
		getRandomIds = {
			artistDao.getArtistIdsByQuery(
				listType.toSqlQuery(
					filters = filters,
					idsOnly = true
				)
			)
		},
		getEntityId = { it.artistId },
		fetchByIds = artistDao::getArtistsByIds,
		getStandardQuery = { artistDao.getArtistsByQuery(listType.toSqlQuery(reversed, filters)) },
		toDomain = { it.toDomainModel() }
	)

	private suspend fun refreshLocalData(
		listType: DomainArtistListType,
		filters: Set<DomainFilter> = emptySet()
	): Flow<PagingData<DomainArtist>> {
		dbRepository.syncArtists().getOrThrow()
		return getLocalData(listType, filters)
	}

	suspend fun getArtists(
		fullRefresh: Boolean,
		listType: DomainArtistListType,
		filters: Set<DomainFilter> = emptySet()
	): Flow<PagingData<DomainArtist>> {
		val localData = getLocalData(listType, filters)
		return if (fullRefresh) {
			refreshLocalData(listType, filters)
		} else {
			localData
		}
	}

	suspend fun isArtistStarred(artistId: String) = artistDao.isArtistStarred(artistId)

	suspend fun starArtist(artistId: String) {
		artistDao.updateArtistStarredAt(artistId, Clock.System.now())
		syncManager.enqueueAction(SyncActionType.STAR, artistId)
	}

	suspend fun unstarArtist(artistId: String) {
		artistDao.updateArtistStarredAt(artistId, null)
		syncManager.enqueueAction(SyncActionType.UNSTAR, artistId)
	}

	fun getArtistLetterCounts(
		filters: Set<DomainFilter> = emptySet()
	): Flow<List<LetterCount>> {
		return artistDao.getArtistLetterCounts(getArtistLetterCountsQuery(filters))
	}
}
