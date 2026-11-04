/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import zavorateam.yabslushal.domain.model.DomainSong
import zavorateam.yabslushal.util.Logger

/**
 * Lyrics source backed by the unofficial Yandex Music API.
 *
 * Flow:
 *  1. `GET /tracks/{trackId}/lyrics?format=LRC` -> signed `downloadUrl`
 *  2. plain `GET` on `downloadUrl` (no Authorization header) -> raw LRC body
 *  3. fallback: `GET /tracks/{trackId}/supplement` -> `result.lyrics.fullLyrics`
 */
interface YandexLyricsSource {
	suspend fun fetchLyrics(song: DomainSong, token: String): String?
}

class YandexMusicLyricsSource(
	private val client: HttpClient,
	private val baseUrl: String = DEFAULT_BASE_URL
) : YandexLyricsSource {

	private val json = Json { ignoreUnknownKeys = true }

	companion object {
		const val DEFAULT_BASE_URL = "https://api.music.yandex.net"
		const val TAG = "YandexLyrics"
	}

	override suspend fun fetchLyrics(song: DomainSong, token: String): String? {
		if (token.isBlank()) return null

		// The Yandex API addresses tracks by its own numeric ids. Yabslushal works with whatever ids
		// the Subsonic server assigns, so the id is only usable when it is numeric.
		val trackId = song.id.takeIf { id -> id.isNotBlank() && id.all(Char::isDigit) } ?: return null

		val fromLrc = runCatching { fetchDownloadUrl(trackId, token) }
			.onFailure { Logger.w(TAG, "lyrics endpoint failed for $trackId", it) }
			.getOrNull()
			?.let { url -> downloadLrc(url) }

		if (!fromLrc.isNullOrBlank()) return fromLrc

		return runCatching { fetchSupplement(trackId, token) }
			.onFailure { Logger.w(TAG, "supplement endpoint failed for $trackId", it) }
			.getOrNull()
	}

	/** Step 1: request the signed download url for the synchronised lyrics. */
	private suspend fun fetchDownloadUrl(trackId: String, token: String): String? {
		val response = client.get("$baseUrl/tracks/$trackId/lyrics") {
			header("Authorization", "OAuth $token")
			parameter("format", "LRC")
			accept(ContentType.Application.Json)
		}

		if (!response.status.isSuccess()) {
			throw IllegalStateException("lyrics request failed with ${response.status.value}")
		}

		val body: JsonObject = json.parseToJsonElement(response.bodyAsText()).jsonObject
		return body["result"]?.jsonObject
			?.get("downloadUrl")?.jsonPrimitive?.contentOrNull
			?.takeIf { it.isNotBlank() }
	}

	/** Step 2: the download url is pre-signed, so no Authorization header is sent. */
	private suspend fun downloadLrc(url: String): String? {
		val response = client.get(url)
		if (response.status == HttpStatusCode.NotFound) return null
		if (!response.status.isSuccess()) {
			throw IllegalStateException("lyrics download failed with ${response.status.value}")
		}
		return response.bodyAsText().takeIf { it.isNotBlank() }
	}

	/** Fallback: the track supplement may carry a plain (unsynchronised) text. */
	private suspend fun fetchSupplement(trackId: String, token: String): String? {
		val response = client.get("$baseUrl/tracks/$trackId/supplement") {
			header("Authorization", "OAuth $token")
			accept(ContentType.Application.Json)
		}

		if (!response.status.isSuccess()) return null

		val body: JsonObject = json.parseToJsonElement(response.bodyAsText()).jsonObject
		val lyrics = body["result"]?.jsonObject?.get("lyrics")
			?.takeIf { it !is kotlinx.serialization.json.JsonNull }
			?.jsonObject
			?: return null

		val fullLyrics = lyrics["fullLyrics"]?.jsonPrimitive?.contentOrNull
			?.takeIf { it.isNotBlank() }

		return fullLyrics ?: lyrics["lyrics"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
	}
}
