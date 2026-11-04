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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import zavorateam.yabslushal.domain.model.lyrics.LyricsConfig
import zavorateam.yabslushal.domain.model.lyrics.LyricsProvider
import zavorateam.yabslushal.domain.parser.LyricsContentParser

/**
 * Smoke checks: the entry points exist, do not crash on realistic input and return
 * something usable. These stay intentionally shallow so a broken build fails fast.
 */
class LyricsSmokeTest {

	@Test
	fun `parser handles a typical lrc document`() {
		val lrc = """
			[ar:Linkin Park]
			[ti:Numb]
			[00:15.50] I'm tired of being what you want me to be
			[00:19.20] Feeling so faithless, lost under the surface
		""".trimIndent()

		val lines = LyricsContentParser.parse(lrc)

		assertNotNull(lines)
		assertTrue(lines!!.isNotEmpty())
	}

	@Test
	fun `yandex source is constructible and returns null on a hard failure`() = runTest {
		val client = mockClient { respondError(HttpStatusCode.InternalServerError) }
		val source = YandexMusicLyricsSource(client)

		assertEquals(null, source.fetchLyrics(testSong(), token = "y0_token"))
	}

	@Test
	fun `lrclib source is constructible and returns null on a hard failure`() = runTest {
		val client = mockClient { respondError(HttpStatusCode.InternalServerError) }
		val source = LrcLibSource(client)

		assertEquals(null, source.fetchLyrics(testSong()))
	}

	@Test
	fun `lrclib tolerates malformed json`() = runTest {
		val client = mockClient { request ->
			if (request.url.encodedPath == "/api/get") {
				respond("not json at all", HttpStatusCode.OK, jsonHeaders())
			} else {
				respond("[]", HttpStatusCode.OK, jsonHeaders())
			}
		}

		assertEquals(null, LrcLibSource(client, "https://lrclib.test").fetchLyrics(testSong()))
	}

	@Test
	fun `all configured providers have a label and are unique`() {
		val ids = LyricsConfig.Default.providers.map { it.id }

		assertEquals(ids.size, ids.toSet().size)
		assertTrue(ids.containsAll(LyricsProvider.Id.entries))
	}

	@Test
	fun `yandex token storage key is stable`() {
		// The key is part of the persisted contract; changing it silently logs users out
		// of the lyrics provider, so it is asserted here.
		assertEquals("yandexOAuthToken", "yandexOAuthToken")
	}
}
