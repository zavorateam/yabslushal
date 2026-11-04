/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.lyrics

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Signature required by the Yandex Music `tracks/{id}/lyrics` endpoint.
 *
 * The endpoint rejects unsigned requests with `400 validate` and a bad signature with
 * `403 Invalid Sign`. The scheme is HMAC-SHA256 over `"{trackId}{timestamp}"` where the message
 * is keyed with a constant taken from the desktop client, and the key is only accepted together
 * with the matching `X-Yandex-Music-Client` header.
 *
 * @property timeStamp unix time in seconds used to build the message
 * @property sign base64 encoded HMAC digest
 */
data class YandexLyricsSignature(
	val timeStamp: Long,
	val sign: String
)

/**
 * Builds request signatures for the Yandex Music lyrics endpoint.
 *
 * @param clock current unix time in seconds, injectable so tests are deterministic
 */
class YandexLyricsSigner(
	private val clock: () -> Long = { System.currentTimeMillis() / 1000 }
) {
	fun sign(trackId: String): YandexLyricsSignature {
		val timeStamp = clock()
		val message = "$trackId$timeStamp"

		val mac = Mac.getInstance("HmacSHA256")
		mac.init(SecretKeySpec(SIGN_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256"))
		val digest: ByteArray = mac.doFinal(message.toByteArray(Charsets.UTF_8))

		val sign = Base64.getEncoder().encodeToString(digest)
		return YandexLyricsSignature(timeStamp = timeStamp, sign = sign)
	}

	companion object {
		/** Key shipped inside the desktop client; valid only with [CLIENT_HEADER]. */
		const val SIGN_KEY = "kzqU4XhfCaY6B6JTHODeq5"

		/** `X-Yandex-Music-Client` value the key above is bound to. */
		const val CLIENT_HEADER = "YandexMusicDesktopAppWindows/5.95.0"

		/** Constant-time comparison helper used by the tests. */
		internal fun matches(
			expected: String,
			actual: String
		): Boolean = MessageDigest.isEqual(
			expected.toByteArray(Charsets.UTF_8),
			actual.toByteArray(Charsets.UTF_8)
		)
	}
}
