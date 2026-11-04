/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for the crash caused by a negative padding modifier.
 *
 * Compose throws `IllegalArgumentException: Padding must be non-negative` as soon as a
 * modifier chain containing a negative [padding] is laid out. The Yandex OAuth field was
 * once added to the connection options screen with `padding(horizontal = (-16).dp)` to
 * compensate for the parent's own inset, which crashed the whole screen on open.
 *
 * These checks are pure Kotlin (no Android runtime needed) so they run as JVM unit tests.
 */
class ConnectionOptionsLayoutTest {

	@Test
	fun `settings screens never request negative horizontal padding`() {
		val source = readSource("ui/screen/settings/ConnectionOptionsScreen.kt")

		assertTrue(
			"negative padding crashes the screen at layout time",
			!source.contains("(-16).dp")
		)
		assertTrue(
			"no negative dp literal of any kind should be used as padding",
			!Regex("""padding\([^)]*\(-\d+\)\.dp""").containsMatchIn(source)
		)
	}

	@Test
	fun `yandex field does not bake in its own horizontal inset`() {
		// The inset belongs to the call site: inside a SettingsGroup the parent already
		// provides it, while the login screen pads its fields explicitly.
		val source = readSource("ui/screen/settings/component/YandexOAuthTokenField.kt")

		assertTrue(
			"field must not hardcode a horizontal padding",
			!source.contains("padding(horizontal = 16.dp)")
		)
	}

	private fun readSource(relative: String): String {
		// The package moved twice already (paige.navic -> zavorateam.yabslushal), so locate the
		// source root by walking the tree instead of hardcoding a package path.
		val root = generateSequence(java.io.File(".").absoluteFile) { it.parentFile }
			.flatMap { dir ->
				sequenceOf(
					java.io.File(dir, "app/src/main/kotlin"),
					java.io.File(dir, "src/main/kotlin")
				)
			}
			.filter { it.isDirectory }
			.firstOrNull()
			?: error("cannot locate a Kotlin source root from ${java.io.File(".").absolutePath}")

		val file = root.walkTopDown()
			.firstOrNull { it.isFile && it.path.endsWith(relative.replace('/', java.io.File.separatorChar)) }
			?: error("cannot locate $relative under $root")
		return file.readText()
	}
}
