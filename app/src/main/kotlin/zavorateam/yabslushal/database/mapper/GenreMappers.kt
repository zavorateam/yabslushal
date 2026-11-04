/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.database.mapper

import zavorateam.yabslushal.database.entity.GenreEntity
import zavorateam.yabslushal.database.relation.GenreWithAlbums
import zavorateam.yabslushal.domain.model.DomainGenre
import dev.zt64.subsonic.api.model.Genre as ApiGenre

fun ApiGenre.toEntity() = GenreEntity(
    genreName = name,
    albumCount = albumCount,
    songCount = songCount
)

fun GenreWithAlbums.toDomainModel() = DomainGenre(
	name = genre.genreName,
	albumCount = genre.albumCount,
	songCount = genre.songCount,
	albums = albums.map { it.toDomainModel() }
)

fun DomainGenre.toEntity() = GenreEntity(
    genreName = name,
    albumCount = albumCount,
    songCount = songCount
)
