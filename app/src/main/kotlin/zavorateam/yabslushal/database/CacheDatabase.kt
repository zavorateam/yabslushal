/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.database

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.RewriteQueriesToDropUnusedColumns
import androidx.room3.RoomDatabase
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import zavorateam.yabslushal.database.dao.AlbumDao
import zavorateam.yabslushal.database.dao.ArtistDao
import zavorateam.yabslushal.database.dao.DownloadDao
import zavorateam.yabslushal.database.dao.GenreDao
import zavorateam.yabslushal.database.dao.LyricDao
import zavorateam.yabslushal.database.dao.PlaylistDao
import zavorateam.yabslushal.database.dao.RadioDao
import zavorateam.yabslushal.database.dao.SongDao
import zavorateam.yabslushal.database.dao.SyncActionDao
import zavorateam.yabslushal.database.entity.AlbumEntity
import zavorateam.yabslushal.database.entity.ArtistEntity
import zavorateam.yabslushal.database.entity.DownloadEntity
import zavorateam.yabslushal.database.entity.GenreEntity
import zavorateam.yabslushal.database.entity.LyricEntity
import zavorateam.yabslushal.database.entity.PlaylistEntity
import zavorateam.yabslushal.database.entity.PlaylistSongCrossRef
import zavorateam.yabslushal.database.entity.RadioEntity
import zavorateam.yabslushal.database.entity.SongEntity
import zavorateam.yabslushal.database.entity.SyncActionEntity

@Database(
	version = 27,
	entities = [
		AlbumEntity::class,
		ArtistEntity::class,
		DownloadEntity::class,
		GenreEntity::class,
		LyricEntity::class,
		PlaylistEntity::class,
		PlaylistSongCrossRef::class,
		RadioEntity::class,
		SongEntity::class,
		SyncActionEntity::class
	],
	autoMigrations = [
		AutoMigration(from = 24, to = 25),
		AutoMigration(from = 25, to = 26),
		AutoMigration(from = 26, to = 27)
	]
)
@ColumnTypeConverters(Converters::class)
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
@RewriteQueriesToDropUnusedColumns
abstract class CacheDatabase : RoomDatabase() {
	abstract fun albumDao(): AlbumDao
	abstract fun artistDao(): ArtistDao
	abstract fun downloadDao(): DownloadDao
	abstract fun genreDao(): GenreDao
	abstract fun lyricDao(): LyricDao
	abstract fun playlistDao(): PlaylistDao
	abstract fun radioDao(): RadioDao
	abstract fun songDao(): SongDao
	abstract fun syncActionDao(): SyncActionDao
}
