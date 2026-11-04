/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.manager.runtime

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import zavorateam.yabslushal.domain.manager.runtime.base.BasePreferenceManager
import zavorateam.yabslushal.domain.model.DomainAlbumListType
import zavorateam.yabslushal.domain.model.DomainArtistListType
import zavorateam.yabslushal.domain.model.DomainPlaylistListType
import zavorateam.yabslushal.domain.model.DomainSongListType
import zavorateam.yabslushal.domain.model.settings.AnimationStyle
import zavorateam.yabslushal.domain.model.settings.AppIconVariant
import zavorateam.yabslushal.domain.model.settings.ArtistAlbumViewMode
import zavorateam.yabslushal.domain.model.settings.BottomBarCollapseMode
import zavorateam.yabslushal.domain.model.settings.BottomBarVisibilityMode
import zavorateam.yabslushal.domain.model.settings.CoverArtProvidersConfig
import zavorateam.yabslushal.domain.model.settings.CoverArtQuality
import zavorateam.yabslushal.domain.model.settings.CoverArtShape
import zavorateam.yabslushal.domain.model.settings.CoverArtTapAction
import zavorateam.yabslushal.domain.model.settings.DiscordStatusType
import zavorateam.yabslushal.domain.model.settings.DownloadDeletionType
import zavorateam.yabslushal.domain.model.settings.DownloadStorage
import zavorateam.yabslushal.domain.model.settings.ExplicitContentPlayback
import zavorateam.yabslushal.domain.model.settings.FontOption
import zavorateam.yabslushal.domain.model.settings.GridSize
import zavorateam.yabslushal.domain.model.settings.ListViewMode
import zavorateam.yabslushal.domain.model.settings.MarqueeSpeed
import zavorateam.yabslushal.domain.model.settings.MiniPlayerProgressStyle
import zavorateam.yabslushal.domain.model.settings.MiniPlayerStyle
import zavorateam.yabslushal.domain.model.settings.NowPlayingBackgroundStyle
import zavorateam.yabslushal.domain.model.settings.NowPlayingSliderStyle
import zavorateam.yabslushal.domain.model.settings.OfflineMode
import zavorateam.yabslushal.domain.model.settings.QueueInfoType
import zavorateam.yabslushal.domain.model.settings.ReplayGainMode
import zavorateam.yabslushal.domain.model.settings.StreamingQuality
import zavorateam.yabslushal.domain.model.settings.Theme
import zavorateam.yabslushal.domain.model.settings.ThemeMode
import zavorateam.yabslushal.domain.model.settings.ToolbarPosition
import zavorateam.yabslushal.playback.exoplayer.impl.listeners.ExoDiscordIntegration
import zavorateam.yabslushal.util.getEncryptedString
import zavorateam.yabslushal.util.insertEncryptedString
import com.russhwolf.settings.Settings as KmpSettings

class PreferenceManager(
	settings: KmpSettings
) : BasePreferenceManager(settings) {
	var queueInfoType by (preference(QueueInfoType.Full))
	var appIconVariant by preference(AppIconVariant.Default)
	var font by preference(FontOption.GoogleSans)
	var fontPath by preference("")
	var animationStyle by preference(AnimationStyle.Expressive)
	var nowPlayingBackgroundStyle by preference(NowPlayingBackgroundStyle.Dynamic)
	var swipeToSkip by preference(true)
	var miniPlayerSwipeToDismiss by preference(true)
	var hideIfIdle by preference(true)
	var enablePredictiveBackAnimations by preference(true)
	var gridSize by preference(GridSize.TwoByTwo)
	var coverArtShape by preference(CoverArtShape.Soft)
	var artistImageShape by preference(CoverArtShape.Soft)
	var coverArtQuality by preference(CoverArtQuality.High)
	var artGridItemSize by preference(150f)
	var marqueeSpeed by preference(MarqueeSpeed.Slow)
	var alphabeticalScroll by preference(false)
	var enableRatings by preference(true)
	var lyricsAutoscroll by preference(true)
	var lyricsBeatByBeat by preference(true)
	var lyricsFakeBeatByBeat by preference(false)
	var lyricsKeepAlive by preference(true)
	var lyricsBlur by preference(false)
	var lyricsBrightInactive by preference(false)
	var enableScrobbling by preference(true)
	var scrobblePercentage by preference(.5f)
	var minDurationToScrobble by preference(30f)
	var replayGainMode by preference(ReplayGainMode.Off)
	var rgAmpGain by preference(0f)
	var ampGain by preference(0f)
	var gaplessPlayback by preference(true)
	var audioOffload by preference(false)
	var floatOutputEnabled by preference(false)

	// TODO: better names and strings for these transcoding settings
	var streamingQualityWifi by preference(StreamingQuality.Lossless)
	var streamingQualityCellular by preference(StreamingQuality.Lossless)
	var isAdvancedTranscodingActive by preference(false)
	var customMaxBitrate by preference(0)
	var customMaxBitrateCellular by preference(0)
	var customFormatWifi by preference("")
	var customFormat by preference("")

	var downloadStorage by preference(DownloadStorage.Internal)
	var downloadQuality by preference(StreamingQuality.Lossless)
	var isAdvancedDownloadTranscodingActive by preference(false)
	var customDownloadMaxBitrate by preference(0)
	var customDownloadFormat by preference("")
	var confirmDeleteTypes by jsonPreference(
		null,
		DownloadDeletionType.entries.toSet()
	)

	fun isConfirmDeleteEnabled(type: DownloadDeletionType): Boolean {
		return confirmDeleteTypes.contains(type)
	}

	fun toggleConfirmDelete(type: DownloadDeletionType) {
		val current = confirmDeleteTypes
		confirmDeleteTypes = if (current.contains(type)) {
			current - type
		} else {
			current + type
		}
	}

	var nowPlayingToolbarPosition by preference(ToolbarPosition.Bottom)
	var nowPlayingSongInfo by preference(true)
	var nowPlayingSliderStyle by preference(NowPlayingSliderStyle.Squiggly)
	var nowPlayingCoverArtAction by preference(CoverArtTapAction.ShowLyrics)
	var nowPlayingShowTimeRemaining by preference(false)
	var customHeaders by preference("")
	var checkForUpdates by preference(true)
	var explicitContentPlayback by preference(ExplicitContentPlayback.Allowed)
	var autoFillQueue by preference(false)
	var shushQueueDuplicateDialog by preference(false)

	// navigation bar settings
	var bottomBarCollapseMode by preference(BottomBarCollapseMode.OnScroll)
	var bottomBarVisibilityMode by preference(BottomBarVisibilityMode.AllScreens)
	var miniPlayerStyle by preference(MiniPlayerStyle.Detached)
	var miniPlayerProgressStyle by preference(MiniPlayerProgressStyle.Seekable)

	// theme related settings
	var theme by preference(Theme.getDefaultTheme())
	var themeMode by preference(ThemeMode.getDefaultMode())
	var amoled by preference(false)
	var dynamicTheming by preference(false)
	var paletteStyle by preference(PaletteStyle.TonalSpot)
	var paletteSpec by preference(ColorSpec.SpecVersion.SPEC_2025)
	var paletteAccentH by preference(0f)
	var paletteAccentS by preference(1f)
	var paletteAccentV by preference(1f)

	// sync related settings
	var lastFullSyncTime by preference(0L)

	// sorting/view mode preferences
	var albumListViewMode by preference(ListViewMode.Grid)
	var playlistListViewMode by preference(ListViewMode.List)
	var artistListViewMode by preference(ListViewMode.List)
	var artistAlbumViewMode by preference(ArtistAlbumViewMode.Grid)

	var albumSortType by jsonPreference<DomainAlbumListType>(DomainAlbumListType.AlphabeticalByArtist)
	var albumSortReversed by preference(false)

	var songSortType by jsonPreference<DomainSongListType>(DomainSongListType.FrequentlyPlayed)
	var songSortReversed by preference(false)

	var artistSortType by preference(DomainArtistListType.AlphabeticalByName)

	var playlistSortType by preference(DomainPlaylistListType.Name)
	var playlistSortReversed by preference(false)

	// these values are bitmasks of `DomainFilter`
	var albumFilters by preference(0)
	var songFilters by preference(0)
	var artistFilters by preference(0)
	var playlistFilters by preference(0)

	var showExtraInFavorites by preference(false)

	fun customHeadersMap(): Map<String, String> = buildMap {
		for (line in customHeaders.lines()) {
			val parts = line.split(":", limit = 2)
			if (parts.size < 2) continue

			val rawKey = parts[0]
			val rawValue = parts[1]

			val key = rawKey.trim()
			val value = rawValue.trim()
			if (key.isNotEmpty() && value.isNotEmpty()) put(key, value)
		}
	}

	var offlineMode by preference(OfflineMode.Auto)

	var enableDiscordIntegration by preference(false)
	var disableRpcWhenPowerSaving by preference(true)
	var discordAppId by preference(ExoDiscordIntegration.DISCORD_APPLICATION_ID)
	var discordStatusType by preference(DiscordStatusType.AppName)
	var discordCoverArtProviders by jsonPreference(CoverArtProvidersConfig.Default)
	var discordCoverArtApiKeyProviders by encryptedJsonPreference<Map<String, String>>(emptyMap())

	var proxyUrl by preference("")
	var dangerousSslNoopEnabled by preference(false)
	var alternateServerAddresses by jsonPreference<List<String>>(emptyList())

	/**
	 * Yandex Music OAuth token (`y0_...`) used by the optional Yandex lyrics provider.
	 * Stored encrypted, the same way as the account password.
	 */
	var yandexOAuthToken
		get() = settings.getEncryptedString(YANDEX_TOKEN_KEY, "")
		set(value) = settings.insertEncryptedString(YANDEX_TOKEN_KEY, value.ifBlank { null })

	private companion object {
		const val YANDEX_TOKEN_KEY = "yandexOAuthToken"
	}
}
