/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.mix

import zavorateam.yabslushal.domain.model.DomainSong
import kotlin.math.abs
import kotlin.random.Random

/**
 * Feature vector extracted from a [DomainSong].
 *
 * Only facts that actually exist in the local database are modelled. There are deliberately no
 * inferred audio attributes (energy, loudness, instrumentalness, ...) - using them would mean
 * inventing data. Every feature is nullable: a missing value is skipped when comparing two songs
 * and the remaining features are re-weighted instead, never treated as zero.
 *
 * @property songId identifier of the source song
 * @property artistKey normalized primary artist
 * @property artistKeys normalized set of all credited artists
 * @property albumArtistKeys normalized set of album artists
 * @property albumId album identity
 * @property titleTokens normalized title tokens without version markers
 * @property genreKeys normalized genres
 * @property moodKeys normalized moods
 * @property contributorKeys normalized production credits
 * @property year release year
 * @property bpm tempo if the server reported one
 * @property durationSeconds track length in seconds
 * @property isrcs international recording codes
 * @property musicBrainzId recording identity
 * @property userRating explicit user rating, `null` when the user never rated the song
 * @property playCount how often the song was played
 * @property starredAtEpoch whether the user starred the song
 */
data class MixFeatures(
	val songId: String,
	val artistKey: String?,
	val artistKeys: Set<String>,
	val albumArtistKeys: Set<String>,
	val albumId: String?,
	val titleTokens: Set<String>,
	val genreKeys: Set<String>,
	val moodKeys: Set<String>,
	val contributorKeys: Set<String>,
	val year: Int?,
	val bpm: Int?,
	val durationSeconds: Int?,
	val isrcs: Set<String>,
	val musicBrainzId: String?,
	val userRating: Int?,
	val playCount: Int,
	val starredAtEpoch: Boolean
) {
	companion object {
		/** Title tokens that only distinguish different versions of the same recording. */
		private val VERSION_TOKENS = setOf(
			"remaster", "remastered", "remastering",
			"live", "version", "edit", "remix", "mix",
			"acoustic", "instrumental", "demo", "mono", "stereo",
			"deluxe", "bonus", "radio", "extended", "reprise"
		)

		/** Splits a free form string into comparable lowercase tokens. */
		internal fun tokens(value: String?): Set<String> {
			if (value.isNullOrBlank()) return emptySet()
			return value
				.lowercase()
				.split(Regex("[^\\p{L}\\p{N}]+"))
				.filter { it.length > 1 }
				.toSet()
		}

		/**
		 * Normalizes an artist or genre name. Whitespace, case and punctuation differences must
		 * not turn two spellings of the same act into different artists.
		 */
		internal fun key(value: String?): String? {
			if (value.isNullOrBlank()) return null
			val normalized = value.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), "").trim()
			return normalized.ifEmpty { null }
		}

		private fun titleTokens(title: String): Set<String> {
			val raw = tokens(title)
			// strip bracketed version markers before tokenizing
			val withoutVersions = raw - VERSION_TOKENS
			return withoutVersions.ifEmpty { raw }
		}

		fun from(song: DomainSong): MixFeatures {
			val genres = buildSet {
				song.genre?.let { key(it) }?.let(::add)
				song.genres.forEach { key(it)?.let(::add) }
			}
			val moods = song.moods.mapNotNull { key(it) }.toSet()
			val contributors = song.contributors.mapNotNull { key(it.artistName) }.toSet()
			val artists = song.artists.mapNotNull { key(it.name) }.toSet()
			val albumArtists = song.albumArtists.mapNotNull { key(it.name) }.toSet()

			return MixFeatures(
				songId = song.id,
				artistKey = key(song.artistName) ?: artists.firstOrNull(),
				artistKeys = artists,
				albumArtistKeys = albumArtists,
				albumId = song.albumId,
				titleTokens = titleTokens(song.title),
				genreKeys = genres,
				moodKeys = moods,
				contributorKeys = contributors,
				year = song.year?.takeIf { it in 1900..2100 },
				bpm = song.bpm?.takeIf { it in 20..300 },
				durationSeconds = song.duration.inWholeSeconds.toInt().takeIf { it > 0 },
				isrcs = song.isrc.mapNotNull { key(it) }.toSet(),
				musicBrainzId = song.musicBrainzId?.takeIf { it.isNotBlank() },
				userRating = song.userRating,
				playCount = song.playCount,
				starredAtEpoch = song.starredAt != null
			)
		}
	}
}

/**
 * Local, feature aware queue builder for the Mix It Up mode.
 *
 * The engine works on a bounded candidate pool of [poolSize] songs per refill cycle. It builds a
 * similarity space out of metadata that is actually stored locally, groups the pool into medoid
 * based clusters, then walks the clusters with two tunable random knobs:
 *
 *  - [variability] widens or narrows how far a pick may stray from its cluster centre,
 *  - [switchRate] controls how often the walk jumps to a different (but similar) cluster.
 *
 * Nothing here talks to the network, uses Compose or touches Media3. It is pure Kotlin with an
 * injectable [Random], so results are reproducible with a fixed seed.
 */
class MixItUpEngine(
	private val random: Random = Random.Default
) {

	/**
	 * Extracts features for the candidates, then greedily produces up to [count] song ids.
	 *
	 * @param candidates the bounded candidate pool, already filtered by the caller
	 * @param count how many songs to append to the queue
	 * @param variability allowed spread around the current cluster centre, in `[0, 1]`
	 * @param switchRate probability weight of switching cluster on each step, in `[0, 1]`
	 * @param seedState seeds the walk so the same pool + parameters give the same order
	 */
	fun buildMix(
		candidates: List<DomainSong>,
		count: Int,
		variability: Double,
		switchRate: Double,
		seedState: Long,
		filter: MixFilter = MixFilter.NONE
	): List<String> {
		if (count <= 0 || candidates.isEmpty()) return emptyList()

		val pool = candidates
			.filter { matchesFilter(it, filter) }
			.distinctBy { it.id }
			.take(poolSize)
		if (pool.isEmpty()) return emptyList()

		val features = pool.map(MixFeatures::from)
		if (features.size == 1) return listOf(features.single().songId)

		val v = variability.coerceIn(0.0, 1.0)
		val s = switchRate.coerceIn(0.0, 1.0)
		val rng = Random(seedState)
		val context = ClusterContext(features)

		val picked = ArrayList<String>(count)
		var currentCluster = context.clusters[rng.nextInt(context.clusters.size)]

		while (picked.size < count) {
			// Two independent draws per step: one decides whether to change direction, the
			// other picks the actual song within the allowed spread.
			val switchDraw = rng.nextDouble()
			val pickDraw = rng.nextDouble()

			if (switchDraw < s) {
				currentCluster = context.nextCluster(currentCluster, rng)
			}

			val choice = context.pickWithinCluster(
				cluster = currentCluster,
				taken = picked.toSet(),
				variability = v,
				draw = pickDraw,
				rng = rng
			)

			if (choice != null) {
				picked += choice.songId
				continue
			}

			// The current cluster is exhausted: move on to the most similar remaining one.
			val fallback = context.remainingClusters(currentCluster, picked.toSet())
			if (fallback.isEmpty()) break
			currentCluster = fallback.first()
		}

		return picked
	}

	companion object {
		/**
		 * Upper bound of the candidate pool for a single refill. Keeps the similarity pass linear
		 * in a small constant instead of scanning the whole library.
		 */
		const val poolSize = 256

		/**
		 * Applies the hard requirements of [filter] to a single song.
		 *
		 * Genres and moods must overlap, the duration and tempo buckets must match, and a seed artist
		 * keeps only that artist. A seed song is not filtered out here: it is handled as a starting
		 * point by the caller so the mix can drift away from it.
		 */
		fun matchesFilter(
			song: DomainSong,
			filter: MixFilter
		): Boolean {
			if (filter.isEmpty) return true

			if (filter.seedArtist != null && !filter.seedArtist.isBlank()) {
				val seedKey = MixFeatures.key(filter.seedArtist)
				val songKeys = buildSet {
					MixFeatures.key(song.artistName)?.let(::add)
					song.artists.forEach { MixFeatures.key(it.name)?.let(::add) }
					song.albumArtists.forEach { MixFeatures.key(it.name)?.let(::add) }
				}
				if (seedKey != null && seedKey !in songKeys) return false
			}

			if (filter.genres.isNotEmpty()) {
				val songGenres = buildSet {
					song.genre?.let { MixFeatures.key(it) }?.let(::add)
					song.genres.forEach { MixFeatures.key(it)?.let(::add) }
				}
				val wanted = filter.genres.mapNotNull { MixFeatures.key(it) }.toSet()
				if (songGenres.none { it in wanted }) return false
			}

			if (filter.moods.isNotEmpty()) {
				val songMoods = song.moods.mapNotNull { MixFeatures.key(it) }.toSet()
				val wanted = filter.moods.mapNotNull { MixFeatures.key(it) }.toSet()
				if (songMoods.none { it in wanted }) return false
			}

			val seconds = song.duration.inWholeSeconds.toInt()
			if (filter.minDurationSeconds != null && seconds < filter.minDurationSeconds) return false
			if (filter.maxDurationSeconds != null && seconds > filter.maxDurationSeconds) return false

			if (!filter.tempo.matches(song.bpm)) return false

			return true
		}
	}

	/**
	 * Precomputed clusters plus the lookup tables the walk needs.
	 */
	private class ClusterContext(
		private val features: List<MixFeatures>
	) {
		private val similarityCache = HashMap<Long, Double>()

		val clusters: List<Cluster> = cluster()

		private fun pairKey(
			a: Int,
			b: Int
		): Long {
			val lo = minOf(a, b).toLong()
			val hi = maxOf(a, b).toLong()
			return (lo shl 32) or hi
		}

		fun similarity(
			a: Int,
			b: Int
		): Double {
			if (a == b) return 1.0
			val key = pairKey(a, b)
			return similarityCache.getOrPut(key) {
				MixSimilarity.between(features[a], features[b])
			}
		}

		/**
		 * Bounded medoid based clustering: seed with the first fully featured song, gather the
		 * songs that are close enough to it, then repeat with whatever is left. A linear pass
		 * with a fixed similarity threshold, so it never becomes quadratic over the whole library.
		 */
		private fun cluster(): List<Cluster> {
			val remaining = features.indices.toMutableSet()
			val result = ArrayList<Cluster>()

			while (remaining.isNotEmpty()) {
				// Prefer a centre that actually has metadata: a medoid with no features would
				// put every remaining song in one giant cluster.
				val medoid = remaining.maxWithOrNull(
					compareBy<Int> { features[it].artistKeys.size }
						.thenBy { features[it].genreKeys.size }
						.thenBy { features[it].titleTokens.size }
						.thenBy { -it }
				) ?: return result

				val members = ArrayList<Int>()
				val leftovers = ArrayList<Int>()
				for (idx in remaining) {
					// Similarity to the centre decides membership; the threshold keeps the
					// clusters musically coherent instead of "everything is one cluster".
					if (similarity(medoid, idx) >= CLUSTER_THRESHOLD) members += idx else leftovers += idx
				}

				remaining.clear()
				remaining.addAll(leftovers)

				result += Cluster(
					medoidIndex = medoid,
					memberIndices = members.sortedByDescending { similarity(medoid, it) }
				)
			}

			return result
		}

		/** Similarity between two clusters, measured at their medoids. */
		fun clusterSimilarity(
			from: Cluster,
			to: Cluster
		): Double = similarity(from.medoidIndex, to.medoidIndex)

		/**
		 * Picks the next cluster. A uniform jump would destroy musical coherence, so candidates
		 * are weighted by their similarity to the current direction and by user affinity.
		 */
		fun nextCluster(
			current: Cluster,
			rng: Random
		): Cluster {
			val others = clusters.filter { it !== current }
			if (others.isEmpty()) return current

			val weights = others.map { candidate ->
				val similarityWeight = clusterSimilarity(current, candidate)
				val affinity = affinityOf(candidate)
				// Even a dissimilar cluster keeps a small weight so the walk can explore.
				0.05 + 0.75 * similarityWeight + 0.20 * affinity
			}
			return weightedPick(others, weights, rng.nextDouble()) ?: current
		}

		/** Clusters that still have songs the caller has not taken yet. */
		fun remainingClusters(
			preferred: Cluster,
			taken: Set<String>
		): List<Cluster> = clusters
			.filter { cluster -> cluster.memberIndices.any { features[it].songId !in taken } }
			.sortedByDescending { clusterSimilarity(preferred, it) }

		/**
		 * Chooses a song inside [cluster] respecting [variability].
		 *
		 * The cluster members are ordered by their similarity to the medoid. [variability] = 0
		 * restricts the choice to the closest half, [variability] = 1 opens the whole cluster up.
		 */
		fun pickWithinCluster(
			cluster: Cluster,
			taken: Set<String>,
			variability: Double,
			draw: Double,
			rng: Random
		): MixFeatures? {
			val available = cluster.memberIndices.filter { features[it].songId !in taken }
			if (available.isEmpty()) return null

			// Depth window: 0.0 -> only the very centre, 1.0 -> the entire cluster.
			val fraction = if (available.size <= 2) 1.0 else 0.35 + 0.65 * variability
			val window = (available.size * fraction).toInt().coerceIn(1, available.size)
			val windowed = available.take(window)

			val weights = windowed.map { index ->
				val closeness = similarity(cluster.medoidIndex, index).coerceIn(0.0, 1.0)
				val affinity = affinityOf(index)
				// Low variability leans on closeness; high variability flattens the field.
				val closenessWeight = 1.0 - variability * 0.6
				val base = (1.0 - closenessWeight) + closenessWeight * closeness
				// User affinity is deliberately strong: a starred, well rated song should show up
				// noticeably more often than an unknown one, not just marginally.
				base * (1.0 + 1.5 * affinity)
			}

			val chosenIndex = weightedPick(windowed, weights, draw) ?: run {
				// Degenerate weights (for example every song has no metadata): uniform choice.
				windowed[rng.nextInt(windowed.size)]
			}

			return features[chosenIndex]
		}

		/** Normalized user preference for a single song. */
		private fun affinityOf(index: Int): Double {
			val f = features[index]
			var score = 0.0
			// Missing ratings are neutral, they must not read as a negative verdict.
			f.userRating?.let { rating -> score += (rating.coerceIn(0, 5) / 5.0) * 0.5 }
			if (f.starredAtEpoch) score += 0.3
			// Squash the play count so a heavily repeated song does not dominate forever.
			score += (f.playCount.coerceAtMost(50) / 50.0) * 0.2
			return score.coerceIn(0.0, 1.0)
		}

		/** Average affinity over the songs in the cluster. */
		private fun affinityOf(cluster: Cluster): Double {
			val members = cluster.memberIndices
			if (members.isEmpty()) return 0.0
			return members.sumOf { affinityOf(it) } / members.size
		}

		/**
		 * Weighted selection driven by a single uniform draw. Returns `null` when the weights are
		 * unusable, so the caller can fall back to an unweighted choice.
		 */
		private fun <T> weightedPick(
			items: List<T>,
			weights: List<Double>,
			draw: Double
		): T? {
			val safe = weights.map { if (it.isFinite() && it > 0) it else 0.0 }
			val total = safe.sum()
			if (total <= 0.0 || items.size != safe.size) return null

			var target = draw.coerceIn(0.0, 0.999999) * total
			for (i in items.indices) {
				target -= safe[i]
				if (target <= 0.0) return items[i]
			}
			return items.lastOrNull()
		}

	}

	/**
	 * A group of songs gathered around a medoid song.
	 *
	 * @property medoidIndex index of the centre song in the feature list
	 * @property memberIndices members ordered by descending similarity to the medoid
	 */
	private data class Cluster(
		val medoidIndex: Int,
		val memberIndices: List<Int>
	)
}

/**
 * Weighted similarity between two songs.
 *
 * Categorical and set valued features use weighted Jaccard style overlap, numeric ones use a
 * normalized difference. Features missing on either side are excluded and the remaining weights
 * are renormalized, so sparse metadata degrades gracefully instead of looking "different".
 */
object MixSimilarity {
	private const val W_ARTIST = 2.2
	private const val W_ALBUM_ARTIST = 0.9
	private const val W_ALBUM = 1.1
	private const val W_TITLE = 0.8
	private const val W_GENRE = 1.6
	private const val W_MOOD = 0.7
	private const val W_CONTRIBUTOR = 0.4
	private const val W_YEAR = 0.6
	private const val W_BPM = 0.5
	private const val W_DURATION = 0.4
	private const val W_IDENTITY = 0.3

	fun between(
		a: MixFeatures,
		b: MixFeatures
	): Double {
		if (a.songId == b.songId) return 1.0

		var weightedSum = 0.0
		var totalWeight = 0.0

		fun add(
			weight: Double,
			value: Double?
		) {
			if (value == null) return
			weightedSum += weight * value.coerceIn(0.0, 1.0)
			totalWeight += weight
		}

		add(W_ARTIST, setOverlap(a.artistKeys, b.artistKeys))
		add(W_ALBUM_ARTIST, setOverlap(a.albumArtistKeys, b.albumArtistKeys))
		add(W_ALBUM, equality(a.albumId, b.albumId))
		add(W_TITLE, setOverlap(a.titleTokens, b.titleTokens))
		add(W_GENRE, setOverlap(a.genreKeys, b.genreKeys))
		add(W_MOOD, setOverlap(a.moodKeys, b.moodKeys))
		add(W_CONTRIBUTOR, setOverlap(a.contributorKeys, b.contributorKeys))
		add(W_YEAR, numericCloseness(a.year, b.year, MAX_YEAR_GAP))
		add(W_BPM, numericCloseness(a.bpm, b.bpm, MAX_BPM_GAP))
		add(W_DURATION, numericCloseness(a.durationSeconds, b.durationSeconds, MAX_DURATION_GAP))

		// Two entries describing the same recording must never be both in the queue.
		add(W_IDENTITY, identityOverlap(a, b))

		return if (totalWeight <= 0.0) 0.0 else weightedSum / totalWeight
	}

	/** Zero means "no shared evidence", one means "identical". */
	private fun setOverlap(
		left: Set<String>,
		right: Set<String>
	): Double? {
		if (left.isEmpty() || right.isEmpty()) return null
		val intersection = left.count { it in right }.toDouble()
		val union = (left + right).size.toDouble()
		return if (union == 0.0) null else intersection / union
	}

	private fun equality(
		left: String?,
		right: String?
	): Double? {
		if (left == null || right == null) return null
		return if (left == right) 1.0 else 0.0
	}

	private fun numericCloseness(
		left: Int?,
		right: Int?,
		maxGap: Double
	): Double? {
		if (left == null || right == null) return null
		val gap = abs(left - right).toDouble()
		return (1.0 - gap / maxGap).coerceIn(0.0, 1.0)
	}

	/** Matches identical ISRCs, MusicBrainz ids or a normalized title + artist pair. */
	private fun identityOverlap(
		a: MixFeatures,
		b: MixFeatures
	): Double? {
		val isrcMatch = a.isrcs.isNotEmpty() && b.isrcs.isNotEmpty() && a.isrcs.any { it in b.isrcs }
		if (isrcMatch) return 1.0

		val mbMatch = a.musicBrainzId != null && a.musicBrainzId == b.musicBrainzId
		if (mbMatch) return 1.0

		if (a.titleTokens.isEmpty() || b.titleTokens.isEmpty()) return null
		if (a.artistKey == null || a.artistKey != b.artistKey) return 0.0

		return if (a.titleTokens == b.titleTokens) 1.0 else 0.0
	}

	private const val MAX_YEAR_GAP = 10.0
	private const val MAX_BPM_GAP = 40.0
	private const val MAX_DURATION_GAP = 180.0
}

/** Minimum similarity to a medoid for a song to join its cluster. */
private const val CLUSTER_THRESHOLD = 0.55
