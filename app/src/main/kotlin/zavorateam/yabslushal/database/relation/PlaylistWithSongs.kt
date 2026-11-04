/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.database.relation

import androidx.room3.Embedded
import androidx.room3.Relation
import zavorateam.yabslushal.database.entity.PlaylistEntity
import zavorateam.yabslushal.database.entity.PlaylistSongCrossRef
import zavorateam.yabslushal.database.entity.SongEntity

data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
		entity = PlaylistSongCrossRef::class,
		parentColumns = ["playlistId"],
		entityColumns = ["playlistId"]
	)
	val songs: List<PlaylistSong>
)

data class PlaylistSong(
    @Embedded val crossRef: PlaylistSongCrossRef,
    @Relation(
		parentColumns = ["songId"],
		entityColumns = ["songId"]
	)
	val song: SongEntity
)
