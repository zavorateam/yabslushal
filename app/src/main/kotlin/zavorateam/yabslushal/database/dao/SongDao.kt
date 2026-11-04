/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.database.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RawQuery
import androidx.room3.RoomRawQuery
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import zavorateam.yabslushal.database.entity.SongEntity
import zavorateam.yabslushal.util.Logger
import kotlin.time.Instant

@Dao
interface SongDao {
	@RawQuery
	fun getPagedSongsByQuery(query: RoomRawQuery): PagingSource<Int, SongEntity>

	@Query("""
		SELECT s.* FROM SongEntity s
		INNER JOIN PlaylistSongCrossRef ps ON s.songId = ps.songId
		WHERE ps.playlistId = :playlistId
		ORDER BY ps.position ASC
	""")
	fun getPagedSongsByPlaylistId(playlistId: String): PagingSource<Int, SongEntity>

	@RawQuery
	suspend fun getSongIdsByQuery(query: RoomRawQuery): List<String>

	@Query("SELECT * FROM SongEntity WHERE songId = :songId LIMIT 1")
	suspend fun getSongById(songId: String): SongEntity?

	@Upsert
	suspend fun insertSong(song: SongEntity)

	@Upsert
	suspend fun insertSongs(songs: List<SongEntity>)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertSongsIgnoringConflicts(songs: List<SongEntity>)

	@Query("SELECT SUM(playCount) FROM SongEntity")
	fun getTotalPlayCount(): Flow<Int?>

	@Query("SELECT SUM(duration * playCount) FROM SongEntity")
	fun getTotalListeningTime(): Flow<Long?>

	@Query("SELECT * FROM SongEntity WHERE playCount > 0 ORDER BY (duration * playCount) DESC LIMIT :limit")
	fun getTopSongs(limit: Int): Flow<List<SongEntity>>

	@Query("SELECT * FROM SongEntity WHERE starredAt IS NOT NULL")
	fun getStarredSongs(): Flow<List<SongEntity>>

	@Query("SELECT belongsToAlbumId FROM SongEntity WHERE playCount > 0 GROUP BY belongsToAlbumId ORDER BY SUM(duration * playCount) DESC LIMIT :limit")
	fun getTopAlbumIds(limit: Int): Flow<List<String>>

	@Query("SELECT artistId FROM SongEntity WHERE playCount > 0 GROUP BY artistId ORDER BY SUM(duration * playCount) DESC LIMIT :limit")
	fun getTopArtistIds(limit: Int): Flow<List<String>>

	@Query("SELECT SUM(playCount) FROM SongEntity WHERE artistId = :artistId")
	fun getArtistPlayCount(artistId: String): Flow<Int?>

	@Query("SELECT SUM(duration * playCount) FROM SongEntity WHERE artistId = :artistId")
	fun getArtistListeningTime(artistId: String): Flow<Long?>

	@Query("SELECT SUM(duration * playCount) FROM SongEntity WHERE belongsToAlbumId = :albumId")
	fun getAlbumListeningTime(albumId: String): Flow<Long?>

	@Query("SELECT * FROM SongEntity WHERE belongsToAlbumId = :albumId")
	suspend fun getSongsByAlbumId(albumId: String): List<SongEntity>

	@Query("DELETE FROM SongEntity WHERE songId = :songId")
	suspend fun deleteSong(songId: String)

	// TODO
	@Query("SELECT EXISTS(SELECT 1 FROM SongEntity WHERE songId = :songId AND starredAt IS NOT NULL)")
	suspend fun isSongStarred(songId: String): Boolean

	@Query("SELECT userRating FROM SongEntity WHERE songId = :songId")
	suspend fun getSongRating(songId: String): Int?

	@Query("UPDATE SongEntity SET starredAt = :starredAt WHERE songId = :songId")
	suspend fun updateSongStarredAt(songId: String, starredAt: Instant?)

	@Query("UPDATE SongEntity SET userRating = :userRating WHERE songId = :songId")
	suspend fun updateSongRating(songId: String, userRating: Int?)

	@Query("DELETE FROM SongEntity")
	suspend fun clearAllSongs()

	@Query("SELECT songId FROM SongEntity")
	suspend fun getAllSongIds(): List<String>

	@Query("SELECT * FROM SongEntity WHERE songId IN (:ids)")
	suspend fun getSongsByIds(ids: List<String>): List<SongEntity>

	@Query("SELECT * FROM SongEntity ORDER BY RANDOM() LIMIT :count")
	suspend fun getRandomSongsList(count: Int): List<SongEntity>

	/**
	 * Bounded candidate pool for the Mix It Up engine.
	 *
	 * Random ordering keeps the pool different on every refill while `LIMIT` guarantees the
	 * engine never loads the whole library into memory. Explicit content is filtered afterwards
	 * by the engine, which reads it from the typed enum field.
	 */
	@Query("SELECT * FROM SongEntity ORDER BY RANDOM() LIMIT :count")
	suspend fun getMixCandidatePool(count: Int): List<SongEntity>

	/**
	 * Distinct mood names, comma separated in a single column because the moods live in a
	 * `List<String>` column joined with `||`. Used to populate the Mix It Up dialog.
	 */
	@Query("SELECT GROUP_CONCAT(moods, '||') FROM SongEntity WHERE moods IS NOT NULL AND moods != ''")
	suspend fun getPackedMoods(): String?

	/** Distinct genre names, used to populate the Mix It Up dialog. */
	@Query("SELECT DISTINCT genre FROM SongEntity WHERE genre IS NOT NULL AND genre != '' ORDER BY genre")
	suspend fun getDistinctGenres(): List<String>

	/** Every stored tempo value, used to decide whether the tempo filter is worth showing. */
	@Query("SELECT bpm FROM SongEntity")
	suspend fun getBpmValues(): List<Int?>

	/** Same pool restricted to songs that are actually stored on the device. */
	@Query("SELECT * FROM SongEntity WHERE songId IN (:downloadedIds) ORDER BY RANDOM() LIMIT :count")
	suspend fun getMixCandidatePoolDownloaded(
		count: Int,
		downloadedIds: List<String>
	): List<SongEntity>

	@Query("SELECT * FROM SongEntity WHERE title LIKE '%' || :query || '%' COLLATE NOCASE")
	suspend fun searchSongsList(query: String): List<SongEntity>

	@Transaction
	suspend fun updateSongsByAlbumId(albumId: String, remoteSongs: List<SongEntity>) {
		val remoteIds = remoteSongs.map { it.songId }.toSet()
		getSongsByAlbumId(albumId).forEach { localSong ->
			if (localSong.songId !in remoteIds) {
				Logger.w("SongDao", "song ${localSong.songId} no longer belongs to album $albumId")
				deleteSong(localSong.songId)
			}
		}
		insertSongs(remoteSongs)
	}

	@Transaction
	suspend fun deleteObsoleteSongs(remoteIds: Set<String>) {
		getAllSongIds().forEach { localId ->
			if (localId !in remoteIds) {
				Logger.w("SongDao", "song $localId no longer exists remotely")
				deleteSong(localId)
			}
		}
	}
}
