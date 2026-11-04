/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import zavorateam.yabslushal.domain.model.settings.LibraryLayoutConfig
import zavorateam.yabslushal.domain.model.settings.LibrarySection

class LibraryLayoutConfigTest {

	// ---- defaults -----------------------------------------------------------------------

	@Test
	fun `default config contains every section exactly once`() {
		val default = LibraryLayoutConfig.default
		assertEquals(LibrarySection.Id.entries.toSet(), default.sections.map { it.id }.toSet())
		assertEquals(LibrarySection.Id.entries.size, default.sections.size)
	}

	@Test
	fun `default order matches the canonical order`() {
		assertEquals(
			LibraryLayoutConfig.DEFAULT_ORDER,
			LibraryLayoutConfig.default.sections.map { it.id }
		)
	}

	@Test
	fun `default shows the home sections and hides the extra ones`() {
		val default = LibraryLayoutConfig.default
		assertTrue(default.isVisible(LibrarySection.Id.MIX_IT_UP))
		assertTrue(default.isVisible(LibrarySection.Id.RECENT_ALBUMS))
		assertTrue(default.isVisible(LibrarySection.Id.PLAYLISTS))
		assertTrue(default.isVisible(LibrarySection.Id.ARTISTS))
		assertFalse(default.isVisible(LibrarySection.Id.GENRES))
		assertFalse(default.isVisible(LibrarySection.Id.STATISTICS))
	}

	// ---- serialization ------------------------------------------------------------------

	@Test
	fun `config survives a full encode decode round trip`() {
		val original = LibraryLayoutConfig.default.withMoved(from = 0, to = 3)
		val roundTripped = LibraryLayoutConfig.decode(LibraryLayoutConfig.encode(original))
		assertEquals(original, roundTripped)
	}

	@Test
	fun `sections are persisted by stable key rather than ordinal`() {
		val raw = LibraryLayoutConfig.encode(LibraryLayoutConfig.default)
		// the enum is serialized by its name, so the payload stays readable and version-safe
		assertTrue(raw.contains("\"id\":\"mixItUp\""))
		assertTrue(raw.contains("\"id\":\"statistics\""))
		assertTrue(raw.contains("\"version\":${LibraryLayoutConfig.VERSION}"))
		assertFalse("ordinal based payload would contain a bare list of numbers", raw.contains("\"sections\":[0"))
	}

	@Test
	fun `decode falls back to defaults for null or blank payloads`() {
		assertEquals(LibraryLayoutConfig.default, LibraryLayoutConfig.decode(null))
		assertEquals(LibraryLayoutConfig.default, LibraryLayoutConfig.decode(""))
		assertEquals(LibraryLayoutConfig.default, LibraryLayoutConfig.decode("   "))
	}

	@Test
	fun `decode falls back to defaults for a corrupted payload`() {
		assertEquals(LibraryLayoutConfig.default, LibraryLayoutConfig.decode("{not json"))
		assertEquals(LibraryLayoutConfig.default, LibraryLayoutConfig.decode("[]"))
		assertEquals(LibraryLayoutConfig.default, LibraryLayoutConfig.decode("""{"sections":"oops"}"""))
	}

	// ---- unknown / migrated ids ---------------------------------------------------------

	@Test
	fun `unknown section id in the payload does not break decoding`() {
		val raw = """
			{"sections":[{"id":"brandNewSection","visible":true}],"version":1}
		""".trimIndent()
		// an id we cannot map is rejected, so the whole payload degrades to the defaults
		// instead of silently rendering a random section
		val decoded = LibraryLayoutConfig.decode(raw)
		assertEquals(LibrarySection.Id.entries.size, decoded.sections.size)
		assertEquals(LibrarySection.Id.entries.toSet(), decoded.sections.map { it.id }.toSet())
	}

	@Test
	fun `sections added after the layout was saved are appended hidden`() {
		// simulate an old install that only knew about two sections
		val oldPayload = """
			{"sections":[
				{"id":"playlists","visible":true},
				{"id":"artists","visible":true}
			],"version":0}
		""".trimIndent()
		val decoded = LibraryLayoutConfig.decode(oldPayload)

		assertEquals(
			listOf(LibrarySection.Id.PLAYLISTS, LibrarySection.Id.ARTISTS),
			decoded.sections.take(2).map { it.id }
		)
		assertEquals(LibrarySection.Id.entries.size, decoded.sections.size)
		assertEquals(LibraryLayoutConfig.VERSION, decoded.version)

		// the kept sections keep their saved visibility, the appended ones fall back to defaults
		assertTrue(decoded.isVisible(LibrarySection.Id.PLAYLISTS))
		assertTrue(decoded.isVisible(LibrarySection.Id.ARTISTS))
		assertEquals(
			LibrarySection.Id.RECENT_ALBUMS in LibraryLayoutConfig.DEFAULT_VISIBLE,
			decoded.isVisible(LibrarySection.Id.RECENT_ALBUMS)
		)
		assertEquals(
			LibrarySection.Id.STATISTICS in LibraryLayoutConfig.DEFAULT_VISIBLE,
			decoded.isVisible(LibrarySection.Id.STATISTICS)
		)
	}

	// ---- normalization ------------------------------------------------------------------

	@Test
	fun `duplicated ids are collapsed keeping the first occurrence`() {
		val duplicated = LibraryLayoutConfig(
			sections = listOf(
				LibrarySection(LibrarySection.Id.STATISTICS, visible = false),
				LibrarySection(LibrarySection.Id.ARTISTS, visible = true),
				LibrarySection(LibrarySection.Id.ARTISTS, visible = false),
				LibrarySection(LibrarySection.Id.STATISTICS, visible = true)
			)
		).normalize()

		assertEquals(LibrarySection.Id.entries.size, duplicated.sections.size)
		assertEquals(LibrarySection.Id.entries.size, duplicated.sections.map { it.id }.toSet().size)
		// first occurrence wins: STATISTICS stays hidden, ARTISTS stays visible
		assertEquals(listOf(LibrarySection.Id.STATISTICS, LibrarySection.Id.ARTISTS), duplicated.sections.take(2).map { it.id })
		assertFalse(duplicated.isVisible(LibrarySection.Id.STATISTICS))
		assertTrue(duplicated.isVisible(LibrarySection.Id.ARTISTS))
	}

	@Test
	fun `normalize keeps an empty config from producing an empty screen`() {
		val normalized = LibraryLayoutConfig(sections = emptyList()).normalize()

		assertEquals(LibrarySection.Id.entries.size, normalized.sections.size)
		assertTrue("at least one section has to stay visible", normalized.sections.any { it.visible })
		assertTrue(normalized.isVisible(LibrarySection.Id.MIX_IT_UP))
	}

	@Test
	fun `normalize forces a visible section when everything was hidden`() {
		val allHidden = LibraryLayoutConfig(
			sections = LibrarySection.Id.entries.map { LibrarySection(it, visible = false) }
		).normalize()

		assertEquals(1, allHidden.sections.count { it.visible })
		assertTrue(allHidden.isVisible(LibrarySection.Id.MIX_IT_UP))
	}

	@Test
	fun `normalize is idempotent`() {
		val once = LibraryLayoutConfig(sections = emptyList()).normalize()
		assertEquals(once, once.normalize())
	}

	// ---- mutations ----------------------------------------------------------------------

	@Test
	fun `move reorders sections and preserves visibility`() {
		val before = LibraryLayoutConfig.default
		val moved = before.withMoved(from = 0, to = 3)

		assertEquals(LibrarySection.Id.RECENT_ALBUMS, moved.sections[0].id)
		assertEquals(LibrarySection.Id.PLAYLISTS, moved.sections[1].id)
		assertEquals(LibrarySection.Id.ARTISTS, moved.sections[2].id)
		assertEquals(LibrarySection.Id.MIX_IT_UP, moved.sections[3].id)
		assertEquals(before.sections.size, moved.sections.size)
		assertEquals(
			before.sections.associate { it.id to it.visible },
			moved.sections.associate { it.id to it.visible }
		)
	}

	@Test
	fun `move with out of bounds indexes is a no-op`() {
		val before = LibraryLayoutConfig.default
		assertEquals(before, before.withMoved(from = -1, to = 2))
		assertEquals(before, before.withMoved(from = 2, to = 99))
		assertEquals(before, before.withMoved(from = 1, to = 1))
	}

	@Test
	fun `toggle flips visibility of a single section`() {
		val toggled = LibraryLayoutConfig.default.withToggled(LibrarySection.Id.GENRES)
		assertTrue(toggled.isVisible(LibrarySection.Id.GENRES))
		assertTrue(LibraryLayoutConfig.default.isVisible(LibrarySection.Id.PLAYLISTS))
		assertTrue(toggled.isVisible(LibrarySection.Id.PLAYLISTS))
	}

	@Test
	fun `toggle refuses to hide the last visible section`() {
		val singleVisible = LibraryLayoutConfig.default
			.withToggled(LibrarySection.Id.RECENT_ALBUMS)
			.withToggled(LibrarySection.Id.PLAYLISTS)
			.withToggled(LibrarySection.Id.ARTISTS)

		assertEquals(1, singleVisible.sections.count { it.visible })
		assertTrue(singleVisible.isVisible(LibrarySection.Id.MIX_IT_UP))

		// attempting to hide the last one must not empty the screen
		val attempt = singleVisible.withToggled(LibrarySection.Id.MIX_IT_UP)
		assertEquals(singleVisible, attempt)
		assertEquals(1, attempt.sections.count { it.visible })
	}

	@Test
	fun `reset restores the original order and visibility`() {
		val edited = LibraryLayoutConfig.default
			.withMoved(from = 0, to = 5)
			.withToggled(LibrarySection.Id.GENRES)

		val reset = LibraryLayoutConfig.default

		assertEquals(LibraryLayoutConfig.DEFAULT_ORDER, reset.sections.map { it.id })
		assertFalse(reset.isVisible(LibrarySection.Id.GENRES))
		assertTrue(edited.sections != reset.sections)
	}
}
