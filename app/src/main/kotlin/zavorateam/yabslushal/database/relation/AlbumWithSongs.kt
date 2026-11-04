/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.database.relation

import androidx.room3.Embedded
import androidx.room3.Relation
import zavorateam.yabslushal.database.entity.AlbumEntity
import zavorateam.yabslushal.database.entity.SongEntity

data class AlbumWithSongs(
    @Embedded val album: AlbumEntity,
    @Relation(
		parentColumns = ["albumId"],
		entityColumns = ["belongsToAlbumId"]
	)
	val songs: List<SongEntity>
)
