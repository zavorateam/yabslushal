/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import zavorateam.yabslushal.domain.repository.AlbumRepository
import zavorateam.yabslushal.domain.repository.ArtistRepository
import zavorateam.yabslushal.domain.repository.CollectionRepository
import zavorateam.yabslushal.domain.repository.DbRepository
import zavorateam.yabslushal.domain.repository.GenreRepository
import zavorateam.yabslushal.domain.repository.LyricsRepository
import zavorateam.yabslushal.domain.repository.PlayerStateRepository
import zavorateam.yabslushal.domain.repository.PlaylistRepository
import zavorateam.yabslushal.domain.repository.RadioRepository
import zavorateam.yabslushal.domain.repository.SearchRepository
import zavorateam.yabslushal.domain.repository.ShareRepository
import zavorateam.yabslushal.domain.repository.SongRepository

val repositoryModule = module {
	singleOf(::AlbumRepository)
	singleOf(::ArtistRepository)
	singleOf(::CollectionRepository)
	singleOf(::DbRepository)
	singleOf(::GenreRepository)
	singleOf(::LyricsRepository)
	singleOf(::PlayerStateRepository)
	singleOf(::PlaylistRepository)
	singleOf(::RadioRepository)
	singleOf(::SearchRepository)
	singleOf(::ShareRepository)
	singleOf(::SongRepository)
}
