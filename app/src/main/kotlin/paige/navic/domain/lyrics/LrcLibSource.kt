/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package paige.navic.domain.lyrics

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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import paige.navic.domain.model.DomainSong
import paige.navic.util.Logger
import kotlin.math.abs

/**
 * Lyrics source backed by the open LRCLIB API (https://lrclib.net).
 *
 *  1. `GET /api/get` with exact metadata -> best match for the exact track version
 *  2. on 404, `GET /api/search` with a loose query -> pick the closest result
 *
 * LRCLIB requires an identifying `User-Agent`; the default engine value is rejected with 403.
 */
interface LrcLibLyricsSource {
	suspend fun fetchLyrics(song: DomainSong): String?
}

class LrcLibSource(
	private val client: HttpClient,
	private val baseUrl: String = DEFAULT_BASE_URL,
	private val userAgent: String = DEFAULT_USER_AGENT
) : LrcLibLyricsSource {

	private val json = Json { ignoreUnknownKeys = true }

	companion object {
		const val DEFAULT_BASE_URL = "https://lrclib.net"
		const val DEFAULT_USER_AGENT = "Navic/1.0 (https://github.com/zavorateam/yabslushal)"
		const val TAG = "LrcLibSource"

		/** /api/get results further than this from the local duration are treated as a mismatch. */
		const val DURATION_TOLERANCE_SECONDS = 5

		private val PARENTHETICAL = Regex("""\([^)]*\)""")
		private val REPEATED_WHITESPACE = Regex("""\s+""")
	}

	override suspend fun fetchLyrics(song: DomainSong): String? {
		val durationSeconds = song.duration.inWholeSeconds.toInt()

		val exact = runCatching { fetchExact(song, durationSeconds) }
			.onFailure { Logger.w(TAG, "exact lookup failed for ${song.title}", it) }
			.getOrNull()

		if (!exact.isNullOrBlank()) return exact

		return runCatching { fetchFuzzy(song) }
			.onFailure { Logger.w(TAG, "fuzzy lookup failed for ${song.title}", it) }
			.getOrNull()
	}

	private suspend fun fetchExact(song: DomainSong, durationSeconds: Int): String? {
		val response = client.get("$baseUrl/api/get") {
			header("User-Agent", userAgent)
			parameter("track_name", song.title)
			parameter("artist_name", song.artistName)
			song.albumTitle?.takeIf { it.isNotBlank() }?.let { parameter("album_name", it) }
			if (durationSeconds > 0) parameter("duration", durationSeconds)
			accept(ContentType.Application.Json)
		}

		if (response.status == HttpStatusCode.NotFound) return null
		if (!response.status.isSuccess()) {
			throw IllegalStateException("lrc lib get failed with ${response.status.value}")
		}

		return parseTrack(json.parseToJsonElement(response.bodyAsText()))
	}

	private suspend fun fetchFuzzy(song: DomainSong): String? {
		val query = listOfNotNull(song.artistName, song.title.withoutParentheticals())
			.filter { it.isNotBlank() }
			.joinToString(" ")

		val response = client.get("$baseUrl/api/search") {
			header("User-Agent", userAgent)
			parameter("q", query)
			accept(ContentType.Application.Json)
		}

		if (!response.status.isSuccess()) {
			throw IllegalStateException("lrc lib search failed with ${response.status.value}")
		}

		val results = json.parseToJsonElement(response.bodyAsText()).jsonArray
			.filterIsInstance<JsonObject>()

		val songSeconds = song.duration.inWholeSeconds.toInt()

		// Prefer synced lyrics; among those prefer the entry whose duration is closest.
		val best = results
			.filter { it.lyricsOrNull("syncedLyrics") != null || it.lyricsOrNull("plainLyrics") != null }
			.minByOrNull { entry ->
				val duration = entry["duration"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
				val penalty = if (duration != null && songSeconds > 0) {
					abs(duration - songSeconds)
				} else {
					Double.MAX_VALUE / 2
				}
				val syncedPenalty = if (entry.lyricsOrNull("syncedLyrics") != null) 0.0 else 1000.0
				penalty + syncedPenalty
			}

		return best?.let { parseTrack(it) }
	}

	/** Prefers synchronised lyrics, falls back to plain text. Returns null for instrumentals. */
	private fun parseTrack(element: JsonElement): String? {
		val obj = element as? JsonObject ?: return null

		obj["instrumental"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()?.let {
			if (it) return null
		}

		obj.lyricsOrNull("syncedLyrics")?.let { return it }
		obj.lyricsOrNull("plainLyrics")?.let { return it }
		return null
	}

	private fun JsonObject.lyricsOrNull(key: String): String? =
		this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

	/** Parts of the title often hold producer credits and make LRCLIB miss the search. */
	private fun String.withoutParentheticals(): String =
		replace(PARENTHETICAL, " ")
			.replace(REPEATED_WHITESPACE, " ")
			.trim()
			.ifEmpty { this }
}
