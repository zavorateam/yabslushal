/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import zavorateam.yabslushal.database.dao.SongDao
import zavorateam.yabslushal.database.entity.SyncActionType
import zavorateam.yabslushal.database.mapper.toDomainModel
import zavorateam.yabslushal.database.paging.createPagedFlow
import zavorateam.yabslushal.database.query.toSqlQuery
import zavorateam.yabslushal.domain.manager.session.SyncManager
import zavorateam.yabslushal.domain.mix.MixItUpEngine
import zavorateam.yabslushal.domain.model.DomainFilter
import zavorateam.yabslushal.domain.model.DomainSong
import zavorateam.yabslushal.domain.model.DomainSongListType
import kotlin.time.Clock

class SongRepository(
	private val songDao: SongDao,
	private val dbRepository: DbRepository,
	private val syncManager: SyncManager
) {
	suspend fun getSongById(id: String) = songDao.getSongById(id)?.toDomainModel()

	suspend fun getRandomSongsList(count: Int): List<DomainSong> {
		return songDao.getRandomSongsList(count).map { it.toDomainModel() }
	}

	/** Genres present in the local library, for the Mix It Up filter dialog. */
	suspend fun getGenres(): List<String> = songDao.getDistinctGenres()
		.filter { isReadable(it) }
		.distinct()
		.sorted()

	/** Moods present in the local library, for the Mix It Up filter dialog. */
	suspend fun getMoods(): List<String> = songDao.getPackedMoods()
		?.split("||")
		.orEmpty()
		.filter { isReadable(it) }
		.distinct()
		.sorted()

	/**
	 * Tags are user supplied, and some files carry names mangled in a broken encoding - a
	 * Cyrillic genre decoded as UTF-16 shows up as private use or CJK code points. Such names are
	 * unusable in a picker, so they are filtered out instead of being offered to the user.
	 */
	private fun isReadable(value: String): Boolean {
		val trimmed = value.trim()
		if (trimmed.isEmpty()) return false
		return trimmed.none { char ->
			char.isISOControl() || char in '\uE000'..'\uF8FF' || char in '\u3400'..'\u9FFF'
		}
	}

	/**
	 * True when at least one song carries a usable tempo.
	 *
	 * Navidrome reports `bpm = 0` for every track that has no tempo tag, and most libraries have
	 * none, so the tempo filter cannot match anything and must not be offered as if it could.
	 */
	suspend fun hasTempoTags(): Boolean = songDao.hasTempoTags()

	/**
	 * Bounded candidate pool for the Mix It Up engine.
	 *
	 * @param count pool size, the engine caps it at [MixItUpEngine.poolSize]
	 * @param downloadedIds when not null, only these locally stored songs are considered
	 */
	suspend fun getMixCandidatePool(
		count: Int,
		downloadedIds: List<String>? = null
	): List<DomainSong> {
		val entities = if (downloadedIds == null) {
			songDao.getMixCandidatePool(count)
		} else {
			if (downloadedIds.isEmpty()) return emptyList()
			songDao.getMixCandidatePoolDownloaded(count, downloadedIds)
		}
		return entities.map { it.toDomainModel() }
	}

	private suspend fun getLocalData(
		listType: DomainSongListType,
		reversed: Boolean = false,
		filters: Set<DomainFilter> = emptySet()
	): Flow<PagingData<DomainSong>> = createPagedFlow(
		isRandom = (listType == DomainSongListType.Random),
		getRandomIds = {
			songDao.getSongIdsByQuery(
				listType.toSqlQuery(
					filters = filters,
					idsOnly = true
				)
			)
		},
		getEntityId = { it.songId },
		fetchByIds = songDao::getSongsByIds,
		getStandardQuery = { songDao.getPagedSongsByQuery(listType.toSqlQuery(reversed, filters)) },
		toDomain = { it.toDomainModel() }
	)

	private suspend fun refreshLocalData(
		listType: DomainSongListType,
		reversed: Boolean = false,
		filters: Set<DomainFilter> = emptySet()
	): Flow<PagingData<DomainSong>> {
		dbRepository.syncLibrarySongs().getOrThrow()
		return getLocalData(listType, reversed, filters)
	}

	suspend fun getSongs(
		fullRefresh: Boolean,
		listType: DomainSongListType,
		reversed: Boolean,
		filters: Set<DomainFilter>
	): Flow<PagingData<DomainSong>> {
		val localData = getLocalData(listType, reversed, filters)
		return if (fullRefresh) {
			refreshLocalData(listType, reversed, filters)
		} else {
			localData
		}
	}

	suspend fun isSongStarred(songId: String) = songDao.isSongStarred(songId)
	suspend fun getSongRating(songId: String) = songDao.getSongRating(songId) ?: 0
	suspend fun starSong(songId: String) {
		songDao.updateSongStarredAt(songId, Clock.System.now())
		syncManager.enqueueAction(SyncActionType.STAR, songId)
	}

	suspend fun unstarSong(songId: String) {
		songDao.updateSongStarredAt(songId, null)
		syncManager.enqueueAction(SyncActionType.UNSTAR, songId)
	}

	suspend fun rateSong(songId: String, rating: Int) {
		songDao.updateSongRating(songId, rating.takeIf { it > 0 })
		when (rating) {
			0 -> syncManager.enqueueAction(SyncActionType.STAR_0, songId)
			1 -> syncManager.enqueueAction(SyncActionType.STAR_1, songId)
			2 -> syncManager.enqueueAction(SyncActionType.STAR_2, songId)
			3 -> syncManager.enqueueAction(SyncActionType.STAR_3, songId)
			4 -> syncManager.enqueueAction(SyncActionType.STAR_4, songId)
			5 -> syncManager.enqueueAction(SyncActionType.STAR_5, songId)
		}
	}
}
