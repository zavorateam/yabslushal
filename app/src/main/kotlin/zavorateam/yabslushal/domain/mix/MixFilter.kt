/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.mix

/**
 * Optional narrowing applied to the Mix It Up candidate pool.
 *
 * Every field is a hard requirement: a song that does not match is excluded before the engine
 * ranks anything. An empty filter - the "I don't care" case - keeps the whole library, which is
 * the previous behaviour.
 *
 * @property genres genres the song must carry at least one of
 * @property moods moods the song must carry at least one of
 * @property minDurationSeconds lower duration bound, inclusive
 * @property maxDurationSeconds upper duration bound, inclusive
 * @property tempo tempo bucket the song must fall into
 * @property seedArtist artist the mix should be built around
 * @property seedSongId single track the mix should be built around
 */
data class MixFilter(
	val genres: Set<String> = emptySet(),
	val moods: Set<String> = emptySet(),
	val minDurationSeconds: Int? = null,
	val maxDurationSeconds: Int? = null,
	val tempo: MixTempo = MixTempo.Any,
	val seedArtist: String? = null,
	val seedSongId: String? = null
) {
	/** True when nothing narrows the library down. */
	val isEmpty: Boolean
		get() = genres.isEmpty() && moods.isEmpty() &&
			minDurationSeconds == null && maxDurationSeconds == null &&
			tempo == MixTempo.Any && seedArtist.isNullOrBlank() && seedSongId.isNullOrBlank()

	companion object {
		val NONE = MixFilter()
	}
}

/**
 * Tempo bucket. Matching uses the real BPM value stored by the server; songs with no BPM are
 * dropped as soon as a specific bucket is requested, because guessing would invent data.
 */
enum class MixTempo(val minBpm: Int?, val maxBpm: Int?) {
	Any(null, null),
	Slow(20, 89),
	Medium(90, 119),
	Fast(120, 300);

	/** True when [bpm] is inside this bucket. [Any] always matches, even without a value. */
	fun matches(bpm: Int?): Boolean {
		if (this == Any) return true
		if (bpm == null) return false
		return bpm in (minBpm ?: 0)..(maxBpm ?: Int.MAX_VALUE)
	}
}

/**
 * Duration buckets offered by the dialog, expressed in seconds.
 */
enum class MixDuration(val minSeconds: Int?, val maxSeconds: Int?) {
	Any(null, null),
	Short(null, 150),
	Medium(150, 240),
	Long(240, null);

	fun matches(seconds: Int?): Boolean {
		if (this == Any) return true
		if (seconds == null) return false
		val min = minSeconds ?: 0
		val max = maxSeconds ?: Int.MAX_VALUE
		return seconds in min..max
	}
}
