/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.model.lyrics

import kotlinx.serialization.Serializable

@Serializable
data class LyricsConfig(
	val providers: List<LyricsProvider>,
	val lyricsPlusMirrors: List<String>,
	val lrcLibBaseUrl: String,
	/** Base url of the Yandex Music API, e.g. https://api.music.yandex.net */
	val yandexBaseUrl: String = DEFAULT_YANDEX_BASE_URL,
	val version: Int
) {
	companion object {
		const val KEY = "lyricsConfig"
		const val VERSION = 2
		const val DEFAULT_YANDEX_BASE_URL = "https://api.music.yandex.net"

		val Default = LyricsConfig(
			providers = listOf(
				LyricsProvider(LyricsProvider.Id.SUBSONIC, true),
				LyricsProvider(LyricsProvider.Id.LRCLIB, false),
				LyricsProvider(LyricsProvider.Id.LYRICS_PLUS, false),
				LyricsProvider(LyricsProvider.Id.YANDEX, false)
			),
			lyricsPlusMirrors = listOf(
				"https://lyricsplus.atomix.one",
				"https://lyricsplus.binimum.org",
				"https://lyricsplus.prjktla.my.id",
				"https://lyrics-plus-backend.vercel.app",
				"https://lyricsplus-seven.vercel.app",
				"https://lyricsplus.prjktla.workers.dev"
			),
			lrcLibBaseUrl = "https://lrclib.net/api/search",
			yandexBaseUrl = DEFAULT_YANDEX_BASE_URL,
			version = VERSION
		)
	}
}
