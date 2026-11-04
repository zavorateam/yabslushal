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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YandexLyricsSignerTest {

	@Test
	fun `signature is deterministic for a fixed clock`() {
		val signer = YandexLyricsSigner(clock = { 1_700_000_000L })

		val first = signer.sign("1626611")
		val second = signer.sign("1626611")

		assertEquals(first, second)
		assertEquals(1_700_000_000L, first.timeStamp)
		assertTrue(first.sign.isNotBlank())
	}

	@Test
	fun `signature matches a known hmac-sha256 vector`() {
		// Computed independently: base64(hmac_sha256("kzqU4XhfCaY6B6JTHODeq5", "16266111700000000")).
		val signer = YandexLyricsSigner(clock = { 1_700_000_000L })

		val signed = signer.sign("1626611")

		assertEquals("C1+vFKtVVpIJIwz7ttnlAP0rYnF9IHPs2fKqinGLyzU=", signed.sign)
	}

	@Test
	fun `different track ids produce different signatures`() {
		val signer = YandexLyricsSigner(clock = { 1_700_000_000L })

		assertTrue(signer.sign("1").sign != signer.sign("2").sign)
	}

	@Test
	fun `timestamp changes with the clock`() {
		var now = 1_700_000_000L
		val signer = YandexLyricsSigner(clock = { now })

		val first = signer.sign("42")
		now += 60
		val second = signer.sign("42")

		assertEquals(1_700_000_060L, second.timeStamp)
		assertTrue(first.sign != second.sign)
	}

	@Test
	fun `lyrics request carries the signature and the client header`() = runTest {
		val signedParams = mutableMapOf<String, String?>()
		var clientHeader: String? = null

		val client = mockClient { request ->
			if (request.url.encodedPath.endsWith("/lyrics")) {
				clientHeader = request.headers["X-Yandex-Music-Client"]
				signedParams["timeStamp"] = request.url.parameters["timeStamp"]
				signedParams["sign"] = request.url.parameters["sign"]
				respond(lrcJson, HttpStatusCode.OK, jsonHeaders())
			} else {
				respond("[00:01.00] Строчка", HttpStatusCode.OK)
			}
		}

		val result = YandexMusicLyricsSource(
			client = client,
			baseUrl = "https://api.test",
			signer = YandexLyricsSigner(clock = { 1_700_000_000L })
		).fetchLyrics(testSong(), token = "y0_token")

		assertEquals("[00:01.00] Строчка", result)
		assertEquals(YandexLyricsSigner.CLIENT_HEADER, clientHeader)
		assertEquals("1700000000", signedParams["timeStamp"])
		assertNotNull(signedParams["sign"])
	}

	@Test
	fun `plus subscription teaser is not shown as lyrics`() = runTest {
		val teaser = "[00:01.00] Подключите подписку на Плюс, чтобы видеть тексты песен  \n[59:59.59] "

		val client = mockClient { request ->
			when {
				request.url.encodedPath.endsWith("/lyrics") ->
					respond(lrcJson, HttpStatusCode.OK, jsonHeaders())

				request.url.encodedPath.endsWith("/supplement") ->
					respond(supplementJson, HttpStatusCode.OK, jsonHeaders())

				else -> respond(teaser, HttpStatusCode.OK)
			}
		}

		val result = YandexMusicLyricsSource(
			client = client,
			baseUrl = "https://api.test",
			signer = YandexLyricsSigner(clock = { 1_700_000_000L })
		).fetchLyrics(testSong(), token = "y0_token")

		// The teaser must be skipped and the real plain text from the supplement used instead.
		assertEquals("Полный текст песни\nКуплет 1", result)
		assertTrue(result?.contains(YandexMusicLyricsSource.SUBSCRIPTION_STUB) == false)
	}

	@Test
	fun `signed endpoint failure still falls back to supplement`() = runTest {
		val client = mockClient { request ->
			when {
				request.url.encodedPath.endsWith("/lyrics") -> respondError(HttpStatusCode.Forbidden)
				request.url.encodedPath.endsWith("/supplement") ->
					respond(supplementJson, HttpStatusCode.OK, jsonHeaders())

				else -> respondError(HttpStatusCode.NotFound)
			}
		}

		val result = YandexMusicLyricsSource(
			client = client,
			baseUrl = "https://api.test",
			signer = YandexLyricsSigner(clock = { 1_700_000_000L })
		).fetchLyrics(testSong(), token = "y0_token")

		assertEquals("Полный текст песни\nКуплет 1", result)
	}

	@Test
	fun `no lyrics anywhere returns null`() = runTest {
		val client = mockClient { request ->
			when {
				request.url.encodedPath.endsWith("/lyrics") ->
					respond(lrcJson, HttpStatusCode.OK, jsonHeaders())

				request.url.encodedPath.endsWith("/supplement") ->
					respond("""{"result":{"id":"1","lyrics":null}}""", HttpStatusCode.OK, jsonHeaders())

				else -> respond("[00:01.00] Подключите подписку на Плюс", HttpStatusCode.OK)
			}
		}

		assertNull(
			YandexMusicLyricsSource(
				client = client,
				baseUrl = "https://api.test",
				signer = YandexLyricsSigner(clock = { 1_700_000_000L })
			).fetchLyrics(testSong(), token = "y0_token")
		)
	}

	private val lrcJson = """
		{"result":{"downloadUrl":"https://cdn.test/l.lrc","lyricId":1}}
	""".trimIndent()

	private val supplementJson = """
		{"result":{"id":"565378","lyrics":{"fullLyrics":"Полный текст песни\nКуплет 1"}}}
	""".trimIndent()
}
