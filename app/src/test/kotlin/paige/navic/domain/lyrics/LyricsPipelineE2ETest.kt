/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package paige.navic.domain.lyrics

import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import paige.navic.domain.parser.LyricsContentParser

/**
 * End-to-end tests that drive a lyrics source through the whole pipeline
 * (HTTP -> raw payload -> [LyricsContentParser] -> timed lines), the same way
 * [paige.navic.domain.repository.LyricsRepository] consumes it.
 */
class LyricsPipelineE2ETest {

	private val yandexLyricsJson = """
		{
		  "invocationInfo": { "req-id": "1" },
		  "result": {
		    "downloadUrl": "https://music-lyrics.s3-private.mds.yandex.net/lyrics/a.lrc",
		    "lyricId": 1, "hasRights": true
		  }
		}
	""".trimIndent()

	private val yandexLrc = """
		[00:15.30] Первая строчка песни
		[00:18.45] Вторая строчка песни
	""".trimIndent()

	private val lrclibJson = """
		{
		  "id": 239482, "trackName": "Numb", "artistName": "Linkin Park",
		  "duration": 187, "instrumental": false,
		  "plainLyrics": "plain",
		  "syncedLyrics": "[00:15.50] I'm tired of being what you want me to be\n[00:19.20] Feeling so faithless"
		}
	""".trimIndent()

	@Test
	fun `yandex lrc flows through parser into timed lyrics lines`() = runTest {
		val client = mockClient { request ->
			if (request.url.encodedPath.endsWith("/lyrics")) {
				respond(yandexLyricsJson, HttpStatusCode.OK, jsonHeaders())
			} else {
				respond(yandexLrc, HttpStatusCode.OK)
			}
		}

		val raw = YandexMusicLyricsSource(client, "https://api.test")
			.fetchLyrics(testSong(), token = "y0_token")
		val lines = LyricsContentParser.parse(raw!!)!!

		assertEquals(2, lines.size)
		assertEquals("Первая строчка песни", lines[0].text)
		assertEquals(15300L, lines[0].time?.inWholeMilliseconds)
		assertEquals(18450L, lines[1].time?.inWholeMilliseconds)
		assertTrue("lyrics are synchronised", lines.all { it.time != null })
	}

	@Test
	fun `yandex supplement plain text yields unsynchronised lines`() = runTest {
		val supplement = """
			{"result":{"id":"565378","lyrics":{"fullLyrics":"Куплет 1\nКуплет 2","hasRights":true}}}
		""".trimIndent()

		val client = mockClient { request ->
			if (request.url.encodedPath.endsWith("/lyrics")) {
				respondError(HttpStatusCode.NotFound)
			} else {
				respond(supplement, HttpStatusCode.OK, jsonHeaders())
			}
		}

		val raw = YandexMusicLyricsSource(client, "https://api.test")
			.fetchLyrics(testSong(), token = "y0_token")
		val lines = LyricsContentParser.parse(raw!!)!!

		assertEquals(listOf("Куплет 1", "Куплет 2"), lines.map { it.text })
		assertTrue("no timestamps expected", lines.all { it.time == null })
	}

	@Test
	fun `lrclib synced lyrics flow through parser`() = runTest {
		val client = mockClient { respond(lrclibJson, HttpStatusCode.OK, jsonHeaders()) }

		val raw = LrcLibSource(client, "https://lrclib.test").fetchLyrics(testSong())
		val lines = LyricsContentParser.parse(raw!!)!!

		assertEquals(2, lines.size)
		assertEquals(15500L, lines[0].time?.inWholeMilliseconds)
		assertEquals(19200L, lines[1].time?.inWholeMilliseconds)
	}

	@Test
	fun `provider fallback chain ends with null when everything fails`() = runTest {
		val client = mockClient { respondError(HttpStatusCode.NotFound) }

		val fromYandex = YandexMusicLyricsSource(client, "https://api.test")
			.fetchLyrics(testSong(), token = "y0_token")
		val fromLrcLib = LrcLibSource(client, "https://lrclib.test").fetchLyrics(testSong())

		assertNull(fromLrcLib)
		assertNull(fromYandex)
	}

	@Test
	fun `yandex result wins over lrclib in a provider chain`() = runTest {
		// Mirrors LyricsRepository: providers are tried in order and the first non-empty
		// result short-circuits the loop.
		val yandexClient = mockClient { request ->
			if (request.url.encodedPath.endsWith("/lyrics")) {
				respond(yandexLyricsJson, HttpStatusCode.OK, jsonHeaders())
			} else {
				respond(yandexLrc, HttpStatusCode.OK)
			}
		}
		val lrclibClient = mockClient { respondError(HttpStatusCode.NotFound) }

		val providers: List<suspend () -> String?> = listOf(
			{ YandexMusicLyricsSource(yandexClient, "https://api.test").fetchLyrics(testSong(), "y0_token") },
			{ LrcLibSource(lrclibClient, "https://lrclib.test").fetchLyrics(testSong()) }
		)

		val winner = providers.firstNotNullOfOrNull { runCatching { it() }.getOrNull() }

		assertEquals(yandexLrc, winner)
		assertFalse("yandex result must be usable", winner!!.isBlank())
	}
}
