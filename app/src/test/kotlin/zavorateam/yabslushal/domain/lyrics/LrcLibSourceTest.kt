/*
 * Navic, a Subsonic music streaming app for Android
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

class LrcLibSourceTest {

	private val exactJson = """
		{
		  "id": 239482,
		  "name": "Numb",
		  "trackName": "Numb",
		  "artistName": "Linkin Park",
		  "albumName": "Meteora",
		  "duration": 187,
		  "instrumental": false,
		  "plainLyrics": "I'm tired of being what you want me to be",
		  "syncedLyrics": "[00:15.50] I'm tired of being what you want me to be\n[00:19.20] Feeling so faithless"
		}
	""".trimIndent()

	private val searchJson = """
		[
		  {
		    "id": 1,
		    "trackName": "Numb",
		    "artistName": "Linkin Park",
		    "duration": 400,
		    "instrumental": false,
		    "plainLyrics": "wrong version",
		    "syncedLyrics": "[00:01.00] wrong version"
		  },
		  {
		    "id": 2,
		    "trackName": "Numb",
		    "artistName": "Linkin Park",
		    "duration": 187,
		    "instrumental": false,
		    "plainLyrics": "right version plain",
		    "syncedLyrics": "[00:15.50] right version"
		  }
		]
	""".trimIndent()

	@Test
	fun `exact get returns synced lyrics`() = runTest {
		var path: String? = null
		var trackName: String? = null
		var artistName: String? = null
		var duration: String? = null
		var userAgent: String? = null

		val client = mockClient { request ->
			path = request.url.encodedPath
			trackName = request.url.parameters["track_name"]
			artistName = request.url.parameters["artist_name"]
			duration = request.url.parameters["duration"]
			userAgent = request.headers["User-Agent"]
			respond(exactJson, HttpStatusCode.OK, jsonHeaders())
		}

		val result = LrcLibSource(client, "https://lrclib.test").fetchLyrics(testSong())

		assertTrue(result!!.startsWith("[00:15.50]"))
		assertEquals("/api/get", path)
		assertEquals("Numb", trackName)
		assertEquals("Linkin Park", artistName)
		assertEquals("187", duration)
		assertEquals(LrcLibSource.DEFAULT_USER_AGENT, userAgent)
	}

	@Test
	fun `falls back to search on 404 and picks closest duration`() = runTest {
		var searchCalled = false

		val client = mockClient { request ->
			when {
				request.url.encodedPath == "/api/get" ->
					respondError(HttpStatusCode.NotFound)

				request.url.encodedPath == "/api/search" -> {
					searchCalled = true
					assertEquals(LrcLibSource.DEFAULT_USER_AGENT, request.headers["User-Agent"])
					respond(searchJson, HttpStatusCode.OK, jsonHeaders())
				}

				else -> respondError(HttpStatusCode.NotFound)
			}
		}

		val result = LrcLibSource(client, "https://lrclib.test").fetchLyrics(testSong())

		assertTrue(searchCalled)
		// duration 187 matches the local track better than 400
		assertEquals("[00:15.50] right version", result)
	}

	@Test
	fun `prefers plain lyrics when synced is null`() = runTest {
		val plainOnly = """
			{"id":1,"instrumental":false,"plainLyrics":"plain text here","syncedLyrics":null}
		""".trimIndent()

		val client = mockClient { respond(plainOnly, HttpStatusCode.OK, jsonHeaders()) }

		assertEquals(
			"plain text here",
			LrcLibSource(client, "https://lrclib.test").fetchLyrics(testSong())
		)
	}

	@Test
	fun `returns null for instrumental tracks`() = runTest {
		val instrumental = """
			{"id":1,"instrumental":true,"plainLyrics":null,"syncedLyrics":null}
		""".trimIndent()

		val client = mockClient { request ->
			if (request.url.encodedPath == "/api/search") {
				respond("[]", HttpStatusCode.OK, jsonHeaders())
			} else {
				respond(instrumental, HttpStatusCode.OK, jsonHeaders())
			}
		}

		assertNull(LrcLibSource(client, "https://lrclib.test").fetchLyrics(testSong()))
	}

	@Test
	fun `returns null when nothing is found`() = runTest {
		val client = mockClient { request ->
			if (request.url.encodedPath == "/api/get") {
				respondError(HttpStatusCode.NotFound)
			} else {
				respond("[]", HttpStatusCode.OK, jsonHeaders())
			}
		}

		assertNull(LrcLibSource(client, "https://lrclib.test").fetchLyrics(testSong()))
	}

	@Test
	fun `search query strips parenthetical title noise`() = runTest {
		var query: String? = null

		val client = mockClient { request ->
			when {
				request.url.encodedPath == "/api/get" -> respondError(HttpStatusCode.NotFound)
				else -> {
					query = request.url.parameters["q"]
					respond("[]", HttpStatusCode.OK, jsonHeaders())
				}
			}
		}

		LrcLibSource(client, "https://lrclib.test")
			.fetchLyrics(testSong(title = "Numb (Remastered 2020)"))

		assertEquals("Linkin Park Numb", query)
	}
}
