/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package paige.navic.domain.lyrics

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import paige.navic.domain.model.lyrics.LyricsConfig
import paige.navic.domain.model.lyrics.LyricsProvider
import paige.navic.domain.parser.LyricsContentParser

class LyricsContentParserTest {

	@Test
	fun `parses lrc with timestamps`() {
		val lines = LyricsContentParser.parse("[00:15.30] first\n[00:18.45] second")!!

		assertEquals(2, lines.size)
		assertEquals("first", lines[0].text)
		assertEquals(15300L, lines[0].time?.inWholeMilliseconds)
		assertEquals("second", lines[1].text)
		assertEquals(18450L, lines[1].time?.inWholeMilliseconds)
	}

	@Test
	fun `parses plain text without timestamps`() {
		val lines = LyricsContentParser.parse("line one\nline two")!!

		assertEquals(2, lines.size)
		assertTrue(lines.all { it.time == null })
		assertEquals("line one", lines[0].text)
	}

	@Test
	fun `prefers synced lyrics in lrclib shaped json`() {
		val json = """{"plainLyrics":"plain","syncedLyrics":"[00:01.00] synced"}"""

		val lines = LyricsContentParser.parse(json)!!

		assertEquals("synced", lines.single().text)
	}

	@Test
	fun `parses plain lyrics when synced missing`() {
		val json = """{"plainLyrics":"a\nb","syncedLyrics":null}"""

		val lines = LyricsContentParser.parse(json)!!

		assertEquals(listOf("a", "b"), lines.map { it.text })
	}

	@Test
	fun `returns null for blank content`() {
		assertNull(LyricsContentParser.parse("   \n  "))
	}

	@Test
	fun `sorts lines by timestamp`() {
		val lines = LyricsContentParser.parse("[00:20.00] later\n[00:10.00] earlier")!!

		assertEquals("earlier", lines[0].text)
		assertEquals("later", lines[1].text)
	}
}

class LyricsConfigTest {

	private val json = Json { ignoreUnknownKeys = true }

	@Test
	fun `default config enables subsonic and lists yandex provider`() {
		val config = LyricsConfig.Default
		val subsonic = config.providers.first { it.id == LyricsProvider.Id.SUBSONIC }

		assertTrue(subsonic.enabled)
		assertTrue(config.providers.any { it.id == LyricsProvider.Id.YANDEX })
		assertEquals("yandex provider is opt in", false, config.providers.first { it.id == LyricsProvider.Id.YANDEX }.enabled)
	}

	@Test
	fun `default config round trips through json`() {
		val encoded = json.encodeToString(LyricsConfig.Default)
		val decoded = json.decodeFromString<LyricsConfig>(encoded)

		assertEquals(LyricsConfig.Default, decoded)
	}

	@Test
	fun `config without yandexBaseUrl still decodes using the default`() {
		// a config persisted by an older app version has no yandexBaseUrl field
		val legacy = """
			{
			  "providers": [{"id": "SUBSONIC", "enabled": true}],
			  "lyricsPlusMirrors": [],
			  "lrcLibBaseUrl": "https://lrclib.net/api/search",
			  "version": 2
			}
		""".trimIndent()

		val decoded = json.decodeFromString<LyricsConfig>(legacy)

		assertEquals(LyricsConfig.DEFAULT_YANDEX_BASE_URL, decoded.yandexBaseUrl)
	}

	@Test
	fun `yandex provider id serializes by name`() {
		val encoded = json.encodeToString(LyricsProvider(LyricsProvider.Id.YANDEX, true))

		assertTrue(encoded.contains("YANDEX"))
	}
}
