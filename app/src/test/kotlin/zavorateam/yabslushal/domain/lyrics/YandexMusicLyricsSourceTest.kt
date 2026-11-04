/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.lyrics

import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YandexMusicLyricsSourceTest {

	private val lyricsJson = """
		{
		  "invocationInfo": { "req-id": "1668329814538869" },
		  "result": {
		    "downloadUrl": "https://music-lyrics.s3-private.mds.yandex.net/lyrics/abc.lrc",
		    "lyricId": 12345,
		    "hasRights": true,
		    "externalLyricId": "12345678",
		    "writers": ["Имя Автора Слов"],
		    "major": { "id": 1, "name": "Musixmatch", "prettyName": "Musixmatch" }
		  }
		}
	""".trimIndent()

	private val supplementJson = """
		{
		  "invocationInfo": { "req-id": "1" },
		  "result": {
		    "id": "565378",
		    "lyrics": {
		      "id": 12345,
		      "lyrics": "Первые строки...",
		      "fullLyrics": "Полный текст песни\nКуплет 1",
		      "hasRights": true,
		      "textLanguage": "ru"
		    }
		  }
		}
	""".trimIndent()

	private val lrcBody = "[00:15.30] Первая строчка песни\n[00:18.45] Вторая строчка песни"

	@Test
	fun `uses lyrics endpoint then downloads signed url and returns lrc`() = runTest {
		val requests = mutableListOf<String>()
		var authHeader: String? = null
		var formatParam: String? = null

		val client = mockClient { request ->
			requests += request.url.toString()
			if (request.url.encodedPath.endsWith("/lyrics")) {
				authHeader = request.headers["Authorization"]
				formatParam = request.url.parameters["format"]
				respond(lyricsJson, HttpStatusCode.OK, jsonHeaders())
			} else {
				// the second hop must NOT carry the oauth header
				assertNull(request.headers["Authorization"])
				respond(lrcBody, HttpStatusCode.OK)
			}
		}

		val result = YandexMusicLyricsSource(client, "https://api.test")
			.fetchLyrics(testSong(), token = "y0_AgAAA")

		assertEquals(lrcBody, result)
		assertEquals("OAuth y0_AgAAA", authHeader)
		assertEquals("LRC", formatParam)
		assertTrue(requests.any { it.contains("/lyrics?") && it.contains("sign=") })
		assertTrue(requests.any { it.contains("s3-private") })
	}

	@Test
	fun `falls back to supplement fullLyrics when lyrics endpoint fails`() = runTest {
		val client = mockClient { request ->
			when {
				request.url.encodedPath.endsWith("/lyrics") ->
					respondError(HttpStatusCode.NotFound)

				request.url.encodedPath.endsWith("/supplement") ->
					respond(supplementJson, HttpStatusCode.OK, jsonHeaders())

				else -> respondError(HttpStatusCode.InternalServerError)
			}
		}

		val result = YandexMusicLyricsSource(client, "https://api.test")
			.fetchLyrics(testSong(), token = "y0_token")

		assertEquals("Полный текст песни\nКуплет 1", result)
	}

	@Test
	fun `falls back to supplement when download url returns 404`() = runTest {
		val client = mockClient { request ->
			when {
				request.url.encodedPath.endsWith("/lyrics") ->
					respond(lyricsJson, HttpStatusCode.OK, jsonHeaders())

				request.url.encodedPath.contains("/lyrics/abc.lrc") ->
					respondError(HttpStatusCode.NotFound)

				request.url.encodedPath.endsWith("/supplement") ->
					respond(supplementJson, HttpStatusCode.OK, jsonHeaders())

				else -> respondError(HttpStatusCode.InternalServerError)
			}
		}

		val result = YandexMusicLyricsSource(client, "https://api.test")
			.fetchLyrics(testSong(), token = "y0_token")

		assertEquals("Полный текст песни\nКуплет 1", result)
	}

	@Test
	fun `returns null when supplement has no lyrics object`() = runTest {
		val emptySupplement = """{"invocationInfo":{},"result":{"id":"565378","lyrics":null}}"""

		val client = mockClient { request ->
			when {
				request.url.encodedPath.endsWith("/lyrics") -> respondError(HttpStatusCode.NotFound)
				request.url.encodedPath.endsWith("/supplement") ->
					respond(emptySupplement, HttpStatusCode.OK, jsonHeaders())
				else -> respondError(HttpStatusCode.NotFound)
			}
		}

		assertNull(
			YandexMusicLyricsSource(client, "https://api.test")
				.fetchLyrics(testSong(), token = "y0_token")
		)
	}

	@Test
	fun `returns null for blank token without touching the network`() = runTest {
		var called = false
		val client = mockClient {
			called = true
			respondError(HttpStatusCode.InternalServerError)
		}

		val result = YandexMusicLyricsSource(client, "https://api.test")
			.fetchLyrics(testSong(), token = "  ")

		assertNull(result)
		assertTrue("network must not be used without a token", !called)
	}

	@Test
	fun `returns null for non numeric track ids which cannot exist in yandex`() = runTest {
		var called = false
		val client = mockClient {
			called = true
			respondError(HttpStatusCode.InternalServerError)
		}

		val result = YandexMusicLyricsSource(client, "https://api.test")
			.fetchLyrics(testSong(id = "tr_abc123"), token = "y0_token")

		assertNull(result)
		assertTrue("non numeric ids must not hit the api", !called)
	}
}
