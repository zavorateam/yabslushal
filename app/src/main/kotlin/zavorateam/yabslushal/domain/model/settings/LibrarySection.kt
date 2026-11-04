/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.model.settings

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/**
 * A single configurable block of the library home screen.
 *
 * @property id stable identifier of the section
 * @property visible whether the section is rendered on the home screen
 */
@Serializable
data class LibrarySection(
	@Serializable(with = LibrarySectionIdSerializer::class)
	val id: Id,
	val visible: Boolean
) {
	/**
	 * Stable identifiers. They are persisted by [key] rather than by ordinal, so inserting or
	 * reordering constants never invalidates an already saved layout.
	 */
	enum class Id(val key: String) {
		MIX_IT_UP("mixItUp"),
		RECENT_ALBUMS("recentAlbums"),
		PLAYLISTS("playlists"),
		ARTISTS("artists"),
		GENRES("genres"),
		STATISTICS("statistics")
	}
}

/**
 * Serializes [LibrarySection.Id] as its stable string [LibrarySection.Id.key].
 *
 * An unknown value cannot be decoded into the enum, so it is rejected by kotlinx.serialization
 * and the whole payload is treated as corrupted by [LibraryLayoutConfig.decode]. That is the
 * desired behaviour: an unreadable layout falls back to the defaults instead of showing
 * arbitrary content.
 */
object LibrarySectionIdSerializer : KSerializer<LibrarySection.Id> {
	override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
		serialName = "zavorateam.yabslushal.domain.model.settings.LibrarySection.Id",
		kind = kotlinx.serialization.descriptors.PrimitiveKind.STRING
	)

	override fun serialize(
		encoder: kotlinx.serialization.encoding.Encoder,
		value: LibrarySection.Id
	) = encoder.encodeString(value.key)

	override fun deserialize(
		decoder: kotlinx.serialization.encoding.Decoder
	): LibrarySection.Id {
		val raw = decoder.decodeString()
		return LibrarySection.Id.entries.first { it.key == raw }
	}
}
