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
import zavorateam.yabslushal.domain.model.DomainExplicitStatus
import zavorateam.yabslushal.domain.model.DomainSong
import zavorateam.yabslushal.domain.model.DomainSongArtist
import kotlin.time.Duration.Companion.seconds

private fun track(
	id: String,
	artist: String = "Artist",
	genres: List<String> = listOf("rock"),
	moods: List<String> = emptyList(),
	bpm: Int? = null,
	durationSeconds: Int = 200
) = DomainSong(
	id = id,
	title = "Song $id",
	artistName = artist,
	artistId = "a1",
	albumTitle = "Album",
	albumId = "al1",
	parentId = null,
	comment = null,
	trackNumber = 1,
	discNumber = 1,
	isrc = emptyList(),
	year = 2000,
	genre = genres.firstOrNull(),
	genres = genres,
	moods = moods,
	duration = durationSeconds.seconds,
	bpm = bpm,
	contributors = emptyList(),
	playCount = 0,
	userRating = null,
	averageRating = null,
	bitRate = null,
	bitDepth = null,
	sampleRate = null,
	audioChannelCount = null,
	replayGain = null,
	fileSize = 0L,
	fileExtension = "mp3",
	mimeType = null,
	filePath = null,
	starredAt = null,
	coverArtId = null,
	musicBrainzId = null,
	explicitStatus = DomainExplicitStatus.Unknown,
	artists = listOf(DomainSongArtist(id = "a1", name = artist)),
	albumArtists = listOf(DomainSongArtist(id = "a1", name = artist)),
	isExternal = false
)

class MixFilterTest {

	@Test
	fun `empty filter matches everything`() {
		assertTrue(MixFilter.NONE.isEmpty)
		assertTrue(MixItUpEngine.matchesFilter(track("1"), MixFilter.NONE))
	}

	@Test
	fun `genre filter keeps only matching songs`() {
		val filter = MixFilter(genres = setOf("rock"))

		assertTrue(MixItUpEngine.matchesFilter(track("1", genres = listOf("rock")), filter))
		assertTrue(MixItUpEngine.matchesFilter(track("2", genres = listOf("pop", "rock")), filter))
		assertFalse(MixItUpEngine.matchesFilter(track("3", genres = listOf("jazz")), filter))
	}

	@Test
	fun `genre matching ignores case and punctuation`() {
		val filter = MixFilter(genres = setOf("Hip-Hop"))

		assertTrue(MixItUpEngine.matchesFilter(track("1", genres = listOf("hip hop")), filter))
	}

	@Test
	fun `mood filter keeps only matching songs`() {
		val filter = MixFilter(moods = setOf("happy"))

		assertTrue(MixItUpEngine.matchesFilter(track("1", moods = listOf("Happy")), filter))
		assertFalse(MixItUpEngine.matchesFilter(track("2", moods = listOf("sad")), filter))
		assertFalse(MixItUpEngine.matchesFilter(track("3"), filter))
	}

	@Test
	fun `duration filter uses inclusive bounds`() {
		val filter = MixFilter(minDurationSeconds = 150, maxDurationSeconds = 240)

		assertTrue(MixItUpEngine.matchesFilter(track("1", durationSeconds = 150), filter))
		assertTrue(MixItUpEngine.matchesFilter(track("2", durationSeconds = 240), filter))
		assertFalse(MixItUpEngine.matchesFilter(track("3", durationSeconds = 149), filter))
		assertFalse(MixItUpEngine.matchesFilter(track("4", durationSeconds = 241), filter))
	}

	@Test
	fun `tempo buckets match real bpm ranges`() {
		assertTrue(MixTempo.Slow.matches(60))
		assertTrue(MixTempo.Medium.matches(100))
		assertTrue(MixTempo.Fast.matches(140))
		assertFalse(MixTempo.Fast.matches(100))
	}

	@Test
	fun `songs without bpm are dropped when a tempo is requested`() {
		// Guessing a tempo would invent data, so an unknown bpm cannot satisfy a specific bucket.
		assertFalse(MixTempo.Medium.matches(null))
		assertTrue(MixTempo.Any.matches(null))
	}

	@Test
	fun `duration buckets map to the documented bounds`() {
		assertTrue(MixDuration.Short.matches(120))
		assertFalse(MixDuration.Short.matches(200))
		assertTrue(MixDuration.Medium.matches(200))
		assertTrue(MixDuration.Long.matches(300))
		assertFalse(MixDuration.Long.matches(200))
		assertTrue(MixDuration.Any.matches(null))
	}

	@Test
	fun `seed artist restricts the pool to that artist`() {
		val filter = MixFilter(seedArtist = "КИНО")

		assertTrue(MixItUpEngine.matchesFilter(track("1", artist = "КИНО"), filter))
		assertTrue(MixItUpEngine.matchesFilter(track("2", artist = "кино"), filter))
		assertFalse(MixItUpEngine.matchesFilter(track("3", artist = "Би-2"), filter))
	}

	@Test
	fun `filters combine as hard requirements`() {
		val filter = MixFilter(
			genres = setOf("rock"),
			minDurationSeconds = 180,
			tempo = MixTempo.Fast
		)

		assertTrue(
			MixItUpEngine.matchesFilter(
				track("1", genres = listOf("rock"), bpm = 140, durationSeconds = 200),
				filter
			)
		)
		assertFalse(
			"right genre but wrong tempo must be rejected",
			MixItUpEngine.matchesFilter(
				track("2", genres = listOf("rock"), bpm = 90, durationSeconds = 200),
				filter
			)
		)
	}

	@Test
	fun `buildMix honours the filter`() {
		val pool = (1..10).map { i ->
			track("r$i", genres = listOf("rock"), bpm = 140)
		} + (1..10).map { i ->
			track("j$i", genres = listOf("jazz"), bpm = 80)
		}

		val mix = MixItUpEngine().buildMix(
			candidates = pool,
			count = 6,
			variability = 0.5,
			switchRate = 0.3,
			seedState = 5,
			filter = MixFilter(genres = setOf("jazz"))
		)

		assertEquals(6, mix.size)
		assertTrue("only jazz songs must appear, got $mix", mix.all { it.startsWith("j") })
	}

	@Test
	fun `filter matching nothing produces an empty mix`() {
		val pool = (1..5).map { track("r$it", genres = listOf("rock")) }

		val mix = MixItUpEngine().buildMix(
			candidates = pool,
			count = 5,
			variability = 0.5,
			switchRate = 0.5,
			seedState = 1,
			filter = MixFilter(genres = setOf("opera"))
		)

		assertTrue(mix.isEmpty())
	}
}
