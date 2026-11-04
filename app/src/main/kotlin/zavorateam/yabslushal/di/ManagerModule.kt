/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.di

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.extractor.metadata.icy.IcyHeaders
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import zavorateam.yabslushal.discord.CoverArtUploadManager
import zavorateam.yabslushal.domain.download.DownloadProcessor
import zavorateam.yabslushal.domain.manager.playback.AudioGainManager
import zavorateam.yabslushal.domain.manager.playback.EqualizerManager
import zavorateam.yabslushal.domain.manager.playback.SleepTimerManager
import zavorateam.yabslushal.domain.manager.playback.StreamCacheManager
import zavorateam.yabslushal.domain.manager.runtime.NotificationManager
import zavorateam.yabslushal.domain.manager.runtime.PermissionManager
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.manager.runtime.StorageManager
import zavorateam.yabslushal.domain.manager.session.connectivity.ConnectivityManager
import zavorateam.yabslushal.domain.manager.session.connectivity.ReachabilityFlow
import zavorateam.yabslushal.domain.manager.session.DownloadManager
import zavorateam.yabslushal.domain.manager.session.LoginManager
import zavorateam.yabslushal.domain.manager.session.SessionManager
import zavorateam.yabslushal.domain.manager.session.SyncManager
import zavorateam.yabslushal.domain.manager.ui.AppIconManager
import zavorateam.yabslushal.domain.manager.ui.LinkManager
import zavorateam.yabslushal.domain.manager.ui.LogManager
import zavorateam.yabslushal.domain.manager.ui.ShareManager
import zavorateam.yabslushal.domain.manager.ui.SnackBarManager
import zavorateam.yabslushal.domain.sync.SyncActionProcessor
import zavorateam.yabslushal.domain.sync.SyncCoordinator
import zavorateam.yabslushal.playback.exoplayer.ExoStateHolder
import zavorateam.yabslushal.playback.exoplayer.impl.cache.NavicDataSource.NavicDataSourceFactory
import zavorateam.yabslushal.util.network.NavicDnsResolver
import zavorateam.yabslushal.util.network.createOkHttpClient
import zavorateam.yabslushal.util.network.getProxyUrlOrNull

val managerModule = module {
	singleOf(::AppIconManager)
	singleOf(::AudioGainManager)
	singleOf(::ConnectivityManager)
	singleOf(::CoverArtUploadManager)
	singleOf(::DownloadManager)
	singleOf(::DownloadProcessor)
	singleOf(::EqualizerManager)
	singleOf(::LinkManager)
	singleOf(::LogManager)
	singleOf(::LoginManager)
	singleOf(::NotificationManager)
	singleOf(::PermissionManager)
	singleOf(::PreferenceManager)
	singleOf(::ReachabilityFlow)
	singleOf(::SessionManager)
	singleOf(::ShareManager)
	singleOf(::SleepTimerManager)
	singleOf(::SnackBarManager)
	singleOf(::StorageManager)
	singleOf(::StreamCacheManager)
	singleOf(::SyncActionProcessor)
	singleOf(::SyncCoordinator)
	single(createdAtStart = true) {
		SyncManager(get(), get(), get(), get(), get())
	}
	single<() -> DataSource.Factory> {
		val preferenceManager: PreferenceManager = get()
		val dnsResolver = get<NavicDnsResolver>()

		@OptIn(UnstableApi::class)
		val provider: () -> DataSource.Factory = {
			// TODO: figure out why KtorDataSource causes #650
			OkHttpDataSource.Factory(
				createOkHttpClient(
					proxyUrl = preferenceManager.proxyUrl.getProxyUrlOrNull(),
					trustAllCerts = preferenceManager.dangerousSslNoopEnabled,
					dnsResolver,
					customHeaders = preferenceManager.customHeadersMap()
				)
			).setDefaultRequestProperties(
				mapOf(
					IcyHeaders.REQUEST_HEADER_ENABLE_METADATA_NAME to
						IcyHeaders.REQUEST_HEADER_ENABLE_METADATA_VALUE
				)
			)
		}
		provider
	}
	single {
		NavicDataSourceFactory(
			downloadManager = get(),
			sessionManager = get(),
			streamCacheManager = get(),
			preferenceManager = get(),
			connectivityManager = get(),
			upstreamFactoryProvider = get()
		)
	}
	singleOf(::ExoStateHolder)
}
