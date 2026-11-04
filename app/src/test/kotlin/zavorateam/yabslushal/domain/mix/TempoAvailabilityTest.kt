/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.mix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tempo filter is offered only when the library really carries tempo values.
 *
 * Navidrome reports `bpm = 0` for every track without a tempo tag, so "the field exists" is not
 * the same as "the data exists". These tests pin the condition that decides visibility.
 */
class TempoAvailabilityTest {

	@Test
	fun `a single real bpm makes the filter available`() {
		assertTrue(MixTempo.hasData(listOf(0, 0, 128)))
	}

	@Test
	fun `zero bpm everywhere keeps the filter hidden`() {
		assertFalse(MixTempo.hasData(listOf(0, 0, 0)))
	}

	@Test
	fun `missing values keep the filter hidden`() {
		assertFalse(MixTempo.hasData(listOf(null, null)))
	}

	@Test
	fun `an empty library keeps the filter hidden`() {
		assertFalse(MixTempo.hasData(emptyList()))
	}

	@Test
	fun `negative or absurd bpm does not count as data`() {
		assertFalse(MixTempo.hasData(listOf(-5, 0)))
	}

	@Test
	fun `buckets still only match what they document`() {
		// The visibility rule must not weaken the matching rule.
		assertTrue(MixTempo.Fast.matches(128))
		assertFalse(MixTempo.Fast.matches(0))
		assertFalse(MixTempo.Fast.matches(null))
		assertEquals(3, MixTempo.entries.count { it != MixTempo.Any })
	}
}
