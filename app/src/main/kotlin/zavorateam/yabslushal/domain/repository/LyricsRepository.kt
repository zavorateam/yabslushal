/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.repository

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import zavorateam.yabslushal.database.dao.LyricDao
import zavorateam.yabslushal.database.dao.SongDao
import zavorateam.yabslushal.database.entity.LyricEntity
import zavorateam.yabslushal.database.mapper.toDomainModel
import zavorateam.yabslushal.domain.lyrics.LrcLibLyricsSource
import zavorateam.yabslushal.domain.lyrics.LrcLibSource
import zavorateam.yabslushal.domain.lyrics.YandexLyricsSource
import zavorateam.yabslushal.domain.lyrics.YandexMusicLyricsSource
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.manager.session.SessionManager
import zavorateam.yabslushal.domain.model.DomainSong
import zavorateam.yabslushal.domain.model.lyrics.LyricsConfig
import zavorateam.yabslushal.domain.model.lyrics.LyricsLine
import zavorateam.yabslushal.domain.model.lyrics.LyricsProvider
import zavorateam.yabslushal.domain.model.lyrics.LyricsResult
import zavorateam.yabslushal.domain.parser.LyricsContentParser
import zavorateam.yabslushal.util.Logger
import kotlin.time.Duration.Companion.milliseconds

class LyricsRepository(
	private val lyricDao: LyricDao,
	private val songDao: SongDao,
	private val settings: Settings,
	private val sessionManager: SessionManager,
	private val preferenceManager: PreferenceManager
) {

	private val client = HttpClient {
		install(HttpTimeout) {
			requestTimeoutMillis = 40000
			connectTimeoutMillis = 40000
			socketTimeoutMillis = 40000
		}
	}
	private val json = Json { ignoreUnknownKeys = true }

	/** LRCLIB: exact `/api/get` lookup first, loose `/api/search` as a fallback. */
	private val lrcLibSource: LrcLibLyricsSource = LrcLibSource(client)

	/** Yandex Music lyrics, requires an OAuth token configured in settings/onboarding. */
	private val yandexSource: YandexLyricsSource = YandexMusicLyricsSource(client)

	private fun getConfig(): LyricsConfig {
		try {
			val raw = settings.getStringOrNull(LyricsConfig.KEY)
				?: return LyricsConfig.Default
			val config: LyricsConfig = json.decodeFromString(raw)
			return config.takeIf { it.version == LyricsConfig.VERSION }
				?: LyricsConfig.Default
		} catch (ex: Exception) {
			Logger.w("LyricsRepository", "failed to load config", ex)
			return LyricsConfig.Default
		}
	}

	suspend fun fetchLyrics(songId: String): LyricsResult? {
		try {
			val cached = lyricDao.getLyrics(songId)
			if (cached != null) {
				val parsed = LyricsContentParser.parse(cached.rawContent)
				if (!parsed.isNullOrEmpty()) return LyricsResult(
					lines = parsed,
					providerName = cached.providerName,
					rawContent = cached.rawContent
				)
			}
		} catch (ex: Exception) {
			Logger.w("LyricsRepository", "failed getting cached lyrics", ex)
		}

		val song = songDao.getSongById(songId)?.toDomainModel() ?: return null

		val currentConfig = getConfig()
		for (provider in currentConfig.providers.filter { it.enabled }) {
			try {
				var rawContentToCache: String? = null

				val parsedLyrics = when (provider.id) {
					LyricsProvider.Id.LYRICS_PLUS -> {
						val raw = fetchRawLyricsPlus(song, currentConfig)
						rawContentToCache = raw
						raw?.let { LyricsContentParser.parse(it) }
					}

					LyricsProvider.Id.LRCLIB -> {
						val raw = lrcLibSource.fetchLyrics(song)
						rawContentToCache = raw
						raw?.let { LyricsContentParser.parse(it) }
					}

					LyricsProvider.Id.YANDEX -> {
						val token = preferenceManager.yandexOAuthToken
						if (token.isBlank()) {
							// not an error: the provider is enabled but never configured
							null
						} else {
							val raw = yandexSource.fetchLyrics(song, token)
							rawContentToCache = raw
							raw?.let { LyricsContentParser.parse(it) }
						}
					}

					LyricsProvider.Id.SUBSONIC -> {
						val subsonicLyrics = sessionManager.api.getLyrics(song.id).firstOrNull()

						val lines = subsonicLyrics?.lines?.flatMap { line ->
							if (!subsonicLyrics.synced && line.value.contains("\n")) {
								line.value.lineSequence()
									.filter { it.isNotBlank() }
									.map { LyricsLine(time = null, text = it.trim()) }
									.toList()
							} else {
								val time =
									if (subsonicLyrics.synced) line.start?.milliseconds else null
								listOf(LyricsLine(time = time, text = line.value))
							}
						}

						if (!lines.isNullOrEmpty()) {
							rawContentToCache = lines.joinToString("\n") { l ->
								val t = l.time
								if (t != null) {
									val m = t.inWholeMinutes.toString().padStart(2, '0')
									val s = (t.inWholeSeconds % 60).toString().padStart(2, '0')
									val ms = ((t.inWholeMilliseconds % 1000) / 10).toString()
										.padStart(2, '0')
									"[$m:$s.$ms]${l.text}"
								} else l.text
							}
						}
						lines
					}
				}

				if (!parsedLyrics.isNullOrEmpty()) {
					try {
						rawContentToCache?.let { content ->
							val entity = LyricEntity(
								songId = song.id,
								providerName = provider.id.name,
								rawContent = content
							)
							lyricDao.insertLyrics(entity)
						}
					} catch (e: Exception) {
						Logger.e("LyricRepository", "Failed to cache lyrics for ${song.title}", e)
					}
					return LyricsResult(
						lines = parsedLyrics,
						providerName = provider.id.name,
						rawContent = rawContentToCache
					)
				}
			} catch (e: Exception) {
				Logger.e("LyricRepository", "Provider ${provider.id.name} failed!", e)
				continue
			}
		}
		return null
	}

	private suspend fun fetchRawLyricsPlus(song: DomainSong, config: LyricsConfig): String? =
		coroutineScope {
			val resultChannel = Channel<String>(Channel.UNLIMITED)
			val jobs = config.lyricsPlusMirrors.map { baseUrl ->
				launch {
					try {
						val response = client.get("$baseUrl/v2/lyrics/get") {
							parameter("title", song.title)
							parameter("artist", song.artistName)
							parameter("album", song.albumTitle)
							parameter("duration", song.duration)
							accept(ContentType.Application.Json)
						}
						if (response.status.isSuccess()) {
							resultChannel.send(response.bodyAsText())
						} else {
							throw Exception("unsuccessful status code ${response.status.value}")
						}
					} catch (ex: Exception) {
						Logger.w("LyricsRepository", "failed fetching from provider $baseUrl", ex)
					}
				}
			}

			launch {
				jobs.joinAll()
				resultChannel.close()
			}

			val result = resultChannel.receiveCatching().getOrNull()
			jobs.forEach { it.cancel() }
			result
		}
}
