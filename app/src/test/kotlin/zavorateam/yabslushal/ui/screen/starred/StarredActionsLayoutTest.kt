/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.starred

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The favorites tab shows a preview of starred songs without the shuffle/play buttons;
 * those live in the full song list reached through "see all". These source-level checks
 * pin that split down so it cannot silently regress.
 */
class StarredActionsLayoutTest {

	@Test
	fun `playback actions are opt in`() {
		val content = readSource("ui/screen/starred/component/Content.kt")

		assertTrue(
			"StarredScreenContent must declare a showPlaybackActions flag",
			content.contains("showPlaybackActions: Boolean = false")
		)
		assertTrue(
			"the shuffle/play row must be gated behind showPlaybackActions",
			content.contains("if (showPlaybackActions) {")
		)
	}

	@Test
	fun `favorites tab does not render the playback actions`() {
		val screen = readSource("ui/screen/starred/StarredScreen.kt")

		assertTrue(
			"favorites tab must not pass showPlaybackActions = true",
			!screen.contains("showPlaybackActions = true")
		)
		assertTrue(
			"favorites tab must not wire shuffle/play callbacks",
			!screen.contains("onPlayAllSongs =") && !screen.contains("onShuffleSongs =")
		)
	}

	@Test
	fun `full song list still offers shuffle and play`() {
		val list = readSource("ui/screen/song/SongListScreen.kt")

		assertTrue(list.contains("Icons.Outlined.Shuffle"))
		assertTrue(list.contains("Icons.Filled.Play"))
	}

	private fun readSource(relative: String): String {
		val root = generateSequence(java.io.File(".").absoluteFile) { it.parentFile }
			.flatMap { dir ->
				sequenceOf(
					java.io.File(dir, "app/src/main/kotlin"),
					java.io.File(dir, "src/main/kotlin")
				)
			}
			.filter { it.isDirectory }
			.firstOrNull()
			?: error("cannot locate a Kotlin source root")

		val file = root.walkTopDown()
			.firstOrNull { it.isFile && it.path.endsWith(relative.replace('/', java.io.File.separatorChar)) }
			?: error("cannot locate $relative under $root")
		return file.readText()
	}
}
