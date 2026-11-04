/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.di

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import zavorateam.yabslushal.domain.model.DomainAlbumListType
import zavorateam.yabslushal.domain.model.DomainArtistListType
import zavorateam.yabslushal.domain.model.DomainFilter
import zavorateam.yabslushal.domain.model.DomainSongListType
import zavorateam.yabslushal.playback.MediaPlayer
import zavorateam.yabslushal.ui.component.dialog.DeletionViewModel
import zavorateam.yabslushal.ui.component.sheet.ChangelogViewModel
import zavorateam.yabslushal.ui.screen.album.viewmodel.AlbumListViewModel
import zavorateam.yabslushal.ui.screen.artist.viewmodel.ArtistDetailViewModel
import zavorateam.yabslushal.ui.screen.artist.viewmodel.ArtistListViewModel
import zavorateam.yabslushal.ui.screen.chat.viewmodel.ChatViewModel
import zavorateam.yabslushal.ui.screen.collection.viewmodel.CollectionDetailViewModel
import zavorateam.yabslushal.ui.screen.genre.viewmodel.GenreListViewModel
import zavorateam.yabslushal.ui.screen.lyrics.viewmodel.LyricsScreenViewModel
import zavorateam.yabslushal.ui.screen.nowPlaying.viewmodel.NowPlayingViewModel
import zavorateam.yabslushal.ui.screen.playlist.viewmodel.PlaylistCreateDialogViewModel
import zavorateam.yabslushal.ui.screen.playlist.viewmodel.PlaylistListViewModel
import zavorateam.yabslushal.ui.screen.playlist.viewmodel.PlaylistUpdateDialogViewModel
import zavorateam.yabslushal.ui.screen.queue.viewmodel.QueueViewModel
import zavorateam.yabslushal.ui.screen.radio.viewmodel.RadioCreateDialogViewModel
import zavorateam.yabslushal.ui.screen.radio.viewmodel.RadioListViewModel
import zavorateam.yabslushal.ui.screen.radio.viewmodel.RadioUpdateDialogViewModel
import zavorateam.yabslushal.ui.screen.search.viewmodel.SearchViewModel
import zavorateam.yabslushal.ui.screen.settings.viewmodel.CoverArtProvidersViewModel
import zavorateam.yabslushal.ui.screen.settings.viewmodel.LibraryLayoutViewModel
import zavorateam.yabslushal.ui.screen.settings.viewmodel.LyricsPriorityViewModel
import zavorateam.yabslushal.ui.screen.settings.viewmodel.NavtabsViewModel
import zavorateam.yabslushal.ui.screen.settings.viewmodel.SettingsDataStorageViewModel
import zavorateam.yabslushal.ui.screen.share.viewmodel.ShareDialogViewModel
import zavorateam.yabslushal.ui.screen.share.viewmodel.ShareListViewModel
import zavorateam.yabslushal.ui.screen.song.viewmodel.SongDetailViewModel
import zavorateam.yabslushal.ui.screen.song.viewmodel.SongListViewModel
import zavorateam.yabslushal.ui.screen.stats.viewmodel.StatisticsViewModel
import zavorateam.yabslushal.ui.viewmodel.RootViewModel
import zavorateam.yabslushal.util.network.YabslushalDnsResolver

val viewModelModule = module {
	single<MediaPlayer> {
		MediaPlayer(
			application = androidApplication(),
			stateRepository = get(),
			songRepository = get(),
			albumDao = get(),
			downloadManager = get(),
			sessionManager = get(),
			preferenceManager = get(),
			snackBarManager = get(),
			audioGainManager = get(),
			streamCacheManager = get(),
			connectivityManager = get(),
			reachability = get()
		)
	}
	single<YabslushalDnsResolver> {
		YabslushalDnsResolver(
			preferenceManager = get(),
			settings = get(),
			connectivityManager = get()
		)
	}

	viewModel { (artistId: String) ->
		ArtistDetailViewModel(
			artistId = artistId,
			repository = get(),
			artistRepository = get(),
			songRepository = get(),
			albumRepository = get(),
			artistDao = get(),
			albumDao = get(),
			reachability = get()
		)
	}

	viewModel { (songId: String?) ->
		LyricsScreenViewModel(
			songId = songId,
			repository = get()
		)
	}

	viewModel { (songIds: List<String>, playlistToExclude: String?) ->
		PlaylistUpdateDialogViewModel(
			songIds = songIds,
			playlistToExclude = playlistToExclude,
			sessionManager = get(),
			snackBarManager = get()
		)
	}

	viewModel { params ->
		AlbumListViewModel(
			initialListType = params.getOrNull<DomainAlbumListType>(),
			initialFilters = params.getOrNull<Set<DomainFilter>>(),
			repository = get(),
			sessionManager = get(),
			preferenceManager = get()
		)
	}
	viewModel { params ->
		SongListViewModel(
			initialListType = params.getOrNull<DomainSongListType>(),
			initialFilters = params.getOrNull<Set<DomainFilter>>(),
			repository = get(),
			sessionManager = get(),
			preferenceManager = get(),
			reachability = get()
		)
	}
	viewModel { params ->
		ArtistListViewModel(
			initialListType = params.getOrNull<DomainArtistListType>(),
			initialFilters = params.getOrNull<Set<DomainFilter>>(),
			repository = get(),
			albumDao = get(),
			sessionManager = get(),
			preferenceManager = get()
		)
	}
	viewModelOf(::SearchViewModel)
	viewModelOf(::GenreListViewModel)
	viewModelOf(::RadioListViewModel)
	viewModelOf(::RadioCreateDialogViewModel)
	viewModel { params ->
		RadioUpdateDialogViewModel(
			initialRadio = params.get(),
			sessionManager = get()
		)
	}
	viewModelOf(::PlaylistListViewModel)
	viewModelOf(::QueueViewModel)
	viewModelOf(::ShareListViewModel)
	viewModelOf(::DeletionViewModel)
	viewModelOf(::ShareDialogViewModel)
	viewModel { (songIds: List<String>) ->
		PlaylistCreateDialogViewModel(
			songIds = songIds,
			playlistDao = get(),
			sessionManager = get(),
			snackBarManager = get()
		)
	}
	viewModel { params ->
		CollectionDetailViewModel(
			collectionId = params.get(),
			repository = get(),
			songRepository = get(),
			albumRepository = get(),
			sessionManager = get(),
			snackBarManager = get(),
			reachability = get()
		)
	}
	viewModelOf(::SongDetailViewModel)
	viewModelOf(::SettingsDataStorageViewModel)
	viewModelOf(::ChangelogViewModel)
	viewModel { params ->
		NowPlayingViewModel(
			player = params.get(),
			songRepository = get()
		)
	}
	viewModelOf(::NavtabsViewModel)
	viewModelOf(::LibraryLayoutViewModel)
	viewModelOf(::CoverArtProvidersViewModel)
	viewModelOf(::LyricsPriorityViewModel)
	viewModelOf(::RootViewModel)
	viewModelOf(::StatisticsViewModel)
	viewModelOf(::ChatViewModel)
}
