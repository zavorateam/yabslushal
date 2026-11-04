/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.mix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import zavorateam.yabslushal.domain.model.DomainArtist
import zavorateam.yabslushal.domain.model.DomainExplicitStatus
import zavorateam.yabslushal.domain.model.DomainSong
import zavorateam.yabslushal.domain.model.DomainSongArtist
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

private fun song(
	id: String,
	title: String = "Song $id",
	artist: String = "Artist $id",
	genres: List<String> = listOf("rock"),
	year: Int? = 2005,
	bpm: Int? = null,
	albumId: String? = "album-$id",
	durationSeconds: Int = 200,
	rating: Int? = null,
	playCount: Int = 0,
	starred: Boolean = false,
	isrc: List<String> = emptyList()
) = DomainSong(
	id = id,
	title = title,
	artistName = artist,
	artistId = "artist-$id",
	albumTitle = "Album $id",
	albumId = albumId,
	parentId = null,
	comment = null,
	trackNumber = 1,
	discNumber = 1,
	isrc = isrc,
	year = year,
	genre = genres.firstOrNull(),
	genres = genres,
	moods = emptyList(),
	duration = durationSeconds.seconds,
	bpm = bpm,
	contributors = emptyList(),
	playCount = playCount,
	userRating = rating,
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
	starredAt = if (starred) kotlin.time.Instant.fromEpochSeconds(1_700_000_000) else null,
	coverArtId = null,
	musicBrainzId = null,
	explicitStatus = DomainExplicitStatus.Unknown,
	artists = listOf(DomainSongArtist(id = "artist-$id", name = artist)),
	albumArtists = listOf(DomainSongArtist(id = "artist-$id", name = artist)),
	isExternal = false
)

class MixItUpEngineTest {

	private fun library(size: Int = 40): List<DomainSong> = (1..size).map { i ->
		song(
			id = "s$i",
			title = "Track $i",
			artist = "Artist ${i % 6}",
			genres = listOf(listOf("rock", "pop", "jazz")[i % 3]),
			year = 1990 + (i % 20),
			bpm = 90 + (i % 40),
			albumId = "album-${i % 5}",
			durationSeconds = 150 + (i % 90)
		)
	}

	@Test
	fun `same seed produces same mix`() {
		val engine = MixItUpEngine(Random(7))
		val pool = library()

		val first = engine.buildMix(pool, count = 10, variability = 0.5, switchRate = 0.3, seedState = 42)
		val second = MixItUpEngine(Random(7))
			.buildMix(pool, count = 10, variability = 0.5, switchRate = 0.3, seedState = 42)

		assertEquals(first, second)
		assertEquals(10, first.size)
	}

	@Test
	fun `different seed changes the order`() {
		val pool = library()
		val a = MixItUpEngine().buildMix(pool, 8, 0.5, 0.3, seedState = 1)
		val b = MixItUpEngine().buildMix(pool, 8, 0.5, 0.3, seedState = 2)

		assertNotEquals(a, b)
	}

	@Test
	fun `mix never repeats a song`() {
		val pool = library()
		val mix = MixItUpEngine().buildMix(pool, count = 30, variability = 0.8, switchRate = 0.7, seedState = 5)

		assertEquals(mix.size, mix.distinct().size)
		assertTrue(mix.all { id -> pool.any { it.id == id } })
	}

	@Test
	fun `mix never returns more songs than the pool holds`() {
		val pool = library(size = 3)
		val mix = MixItUpEngine().buildMix(pool, count = 20, variability = 0.5, switchRate = 0.5, seedState = 3)

		assertEquals(3, mix.size)
	}

	@Test
	fun `empty library gives an empty mix`() {
		assertTrue(MixItUpEngine().buildMix(emptyList(), 10, 0.5, 0.5, 1).isEmpty())
	}

	@Test
	fun `zero count gives an empty mix`() {
		assertTrue(MixItUpEngine().buildMix(library(), 0, 0.5, 0.5, 1).isEmpty())
	}

	@Test
	fun `single song library returns that song`() {
		val only = song(id = "only")
		assertEquals(listOf("only"), MixItUpEngine().buildMix(listOf(only), 5, 0.5, 0.5, 1))
	}

	@Test
	fun `songs without metadata still produce a mix`() {
		val pool = (1..10).map { i ->
			song(id = "sparse$i", title = "", artist = "", genres = emptyList(), year = null, albumId = null)
		}

		val mix = MixItUpEngine().buildMix(pool, 5, 1.0, 1.0, seedState = 9)

		assertEquals(5, mix.size)
		assertEquals(5, mix.distinct().size)
	}

	@Test
	fun `variability zero stays closer to the cluster centre than variability one`() {
		// Two well separated families, so the tight setting must stay inside one of them.
		val rock = (1..12).map { i ->
			song(id = "rock$i", artist = "Rock Band $i", genres = listOf("rock"), year = 1990, albumId = "ra$i")
		}
		val jazz = (1..12).map { i ->
			song(id = "jazz$i", artist = "Jazz Band $i", genres = listOf("jazz"), year = 2020, albumId = "ja$i")
		}
		val pool = rock + jazz

		val tight = MixItUpEngine().buildMix(pool, 6, variability = 0.0, switchRate = 0.0, seedState = 11)
		val wide = MixItUpEngine().buildMix(pool, 6, variability = 1.0, switchRate = 1.0, seedState = 11)

		val tightFamilies = tight.map { if (it.startsWith("rock")) "rock" else "jazz" }.distinct().size
		val wideFamilies = wide.map { if (it.startsWith("rock")) "rock" else "jazz" }.distinct().size

		assertTrue("tight mix must stay in one family, was $tight", tightFamilies == 1)
		assertTrue("wide mix may span both families, was $wide", wideFamilies >= tightFamilies)
	}

	@Test
	fun `switch rate zero does not jump between clusters`() {
		val pool = (1..24).map { i ->
			val family = if (i % 2 == 0) "metal" else "ambient"
			song(
				id = "t$i",
				artist = "$family $i",
				genres = listOf(family),
				year = if (family == "metal") 1985 else 2015,
				albumId = "$family-$i"
			)
		}

		val mix = MixItUpEngine().buildMix(pool, 5, 0.0, switchRate = 0.0, seedState = 13)
		val genres = mix.map { if (it.removePrefix("t").toInt() % 2 == 0) "metal" else "ambient" }.distinct()

		assertEquals(1, genres.size)
	}

	@Test
	fun `similarity is symmetric`() {
		val a = MixFeatures.from(song(id = "a", artist = "X", genres = listOf("rock")))
		val b = MixFeatures.from(song(id = "b", artist = "X", genres = listOf("rock", "pop")))

		assertEquals(MixSimilarity.between(a, b), MixSimilarity.between(b, a), 0.0001)
	}

	@Test
	fun `identical songs are fully similar`() {
		val a = MixFeatures.from(song(id = "x"))
		val same = a.copy(songId = "y")

		assertEquals(1.0, MixSimilarity.between(a, same), 0.0001)
	}

	@Test
	fun `missing features are skipped rather than treated as zero`() {
		// Two songs with nothing in common except the only fields they both carry.
		val a = MixFeatures.from(song(id = "a", genres = listOf("rock"), year = null, albumId = null)).copy(
			artistKeys = emptySet(),
			albumArtistKeys = emptySet(),
			titleTokens = emptySet(),
			genreKeys = setOf("rock"),
			contributorKeys = emptySet()
		)
		val b = a.copy(songId = "b")

		assertEquals(1.0, MixSimilarity.between(a, b), 0.0001)
	}

	@Test
	fun `no shared evidence means no similarity`() {
		// Nothing in common: different artists, genres, albums, titles and years.
		val a = MixFeatures.from(
			song(id = "a", title = "Alpha", artist = "A", genres = listOf("rock"), year = 1990, albumId = "alA")
		).copy(albumArtistKeys = emptySet(), contributorKeys = emptySet(), durationSeconds = null)
		val b = MixFeatures.from(
			song(id = "b", title = "Beta", artist = "B", genres = listOf("jazz"), year = 2020, albumId = "alB")
		).copy(albumArtistKeys = emptySet(), contributorKeys = emptySet(), durationSeconds = null)

		assertEquals(0.0, MixSimilarity.between(a, b), 0.0001)
	}

	@Test
	fun `favourites surface far more often than unrated songs`() {
		val pool = (1..12).map { i -> song(id = "n$i", genres = listOf("rock"), artist = "Same", albumId = "al$i") } +
			song(
				id = "fav",
				genres = listOf("rock"),
				artist = "Same",
				albumId = "alF",
				rating = 5,
				playCount = 40,
				starred = true
			)

		val hits = (1..200).count { seed ->
			MixItUpEngine()
				.buildMix(pool, count = 2, variability = 1.0, switchRate = 0.0, seedState = seed.toLong())
				.contains("fav")
		}
		// Two picks out of thirteen songs is ~15% by pure chance; the affinity boost must clearly
		// beat that without ever being deterministic.
		val baseline = 200.0 * 2 / pool.size

		assertTrue(
			"favourite should be boosted well above chance, got $hits vs baseline $baseline",
			hits > baseline * 1.5
		)
	}

	@Test
	fun `unrated songs are not treated as disliked`() {
		// Every song unrated: the mix must still be produced and cover several songs.
		val pool = (1..20).map { i -> song(id = "u$i", genres = listOf("rock"), artist = "Same", albumId = "al$i") }

		val mix = MixItUpEngine()
			.buildMix(pool, count = 10, variability = 1.0, switchRate = 0.5, seedState = 31)

		assertEquals(10, mix.size)
		assertTrue("unrated pool must not collapse to one song", mix.distinct().size >= 5)
	}

	@Test
	fun `feature extraction normalizes artist spelling`() {
		val a = MixFeatures.from(song(id = "1", artist = "Noize MC"))
		val b = MixFeatures.from(song(id = "2", artist = "noize mc"))

		assertEquals(a.artistKey, b.artistKey)
	}

	@Test
	fun `feature extraction drops version markers from titles`() {
		val plain = MixFeatures.from(song(id = "1", title = "Звезда по имени Солнце"))
		val remaster = MixFeatures.from(song(id = "2", title = "Звезда по имени Солнце (Remastered)"))

		assertEquals(plain.titleTokens, remaster.titleTokens)
	}

	@Test
	fun `out of range bpm and year are ignored`() {
		val weird = MixFeatures.from(song(id = "1", bpm = 0, year = 1800))

		assertEquals(null, weird.bpm)
		assertEquals(null, weird.year)
	}

	@Test
	fun `pool size cap keeps the pass bounded`() {
		val huge = (1..600).map { song(id = "h$it") }

		val mix = MixItUpEngine().buildMix(huge, count = 300, variability = 0.5, switchRate = 0.5, seedState = 21)

		assertTrue("engine must not return more than its pool cap", mix.size <= MixItUpEngine.poolSize)
	}
}
