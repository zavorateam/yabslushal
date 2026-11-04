/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package paige.navic.domain.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import paige.navic.domain.model.DomainExplicitStatus
import paige.navic.domain.model.DomainSong
import kotlin.time.Duration.Companion.seconds

/**
 * Minimal [DomainSong] factory. Only the fields the lyrics sources read are meaningful here;
 * the rest is filled with neutral defaults.
 */
internal fun testSong(
	id: String = "565378",
	title: String = "Numb",
	artist: String? = "Linkin Park",
	album: String? = "Meteora",
	durationSeconds: Int = 187
) = DomainSong(
	id = id,
	title = title,
	artistName = artist,
	artistId = "artist-1",
	albumTitle = album,
	albumId = "album-1",
	parentId = null,
	comment = null,
	trackNumber = 1,
	discNumber = 1,
	isrc = emptyList(),
	year = 2003,
	genre = "Rock",
	genres = emptyList(),
	moods = emptyList(),
	duration = durationSeconds.seconds,
	bpm = null,
	contributors = emptyList(),
	playCount = 0,
	userRating = null,
	averageRating = null,
	bitRate = 320,
	bitDepth = null,
	sampleRate = null,
	audioChannelCount = null,
	replayGain = null,
	fileSize = 0L,
	fileExtension = "mp3",
	mimeType = "audio/mpeg",
	filePath = null,
	starredAt = null,
	coverArtId = null,
	musicBrainzId = null,
	explicitStatus = DomainExplicitStatus.Unknown,
	artists = emptyList(),
	albumArtists = emptyList()
)

/** Builds an [HttpClient] whose engine answers with the provided lambda. */
internal fun mockClient(
	handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
): HttpClient = HttpClient(MockEngine { request -> handler(request) })

internal fun jsonHeaders() =
	headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

