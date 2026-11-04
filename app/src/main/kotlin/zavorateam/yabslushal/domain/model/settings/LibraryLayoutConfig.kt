/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.model.settings

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class LibraryLayoutConfig(
	val sections: List<LibrarySection>,
	val version: Int = VERSION
) {
	val visibleSections: List<LibrarySection.Id>
		get() = sections.filter { it.visible }.map { it.id }

	fun isVisible(id: LibrarySection.Id): Boolean =
		sections.any { it.id == id && it.visible }

	fun normalize(): LibraryLayoutConfig {
		val seen = LinkedHashSet<LibrarySection.Id>()
		val result = ArrayList<LibrarySection>(LibrarySection.Id.entries.size)

		for (section in sections) {
			if (!seen.add(section.id)) continue
			result += section
		}

		for (id in DEFAULT_ORDER) {
			if (seen.add(id)) result += LibrarySection(id, visible = id in DEFAULT_VISIBLE)
		}

		if (result.none { it.visible }) {
			result[0] = result[0].copy(visible = true)
		}

		return LibraryLayoutConfig(sections = result, version = VERSION)
	}

	fun withMoved(from: Int, to: Int): LibraryLayoutConfig {
		if (from == to) return this
		if (from !in sections.indices || to !in sections.indices) return this
		val list = sections.toMutableList()
		list.add(to, list.removeAt(from))
		return copy(sections = list)
	}

	fun withToggled(id: LibrarySection.Id): LibraryLayoutConfig {
		val target = sections.firstOrNull { it.id == id } ?: return this
		if (target.visible && sections.count { it.visible } <= 1) return this
		return copy(
			sections = sections.map {
				if (it.id == id) it.copy(visible = !it.visible) else it
			}
		)
	}

	companion object {
		const val KEY = "libraryLayoutConfig"
		const val VERSION = 1

		val DEFAULT_VISIBLE = setOf(
			LibrarySection.Id.MIX_IT_UP,
			LibrarySection.Id.RECENT_ALBUMS,
			LibrarySection.Id.PLAYLISTS,
			LibrarySection.Id.ARTISTS
		)

		val DEFAULT_ORDER = listOf(
			LibrarySection.Id.MIX_IT_UP,
			LibrarySection.Id.RECENT_ALBUMS,
			LibrarySection.Id.PLAYLISTS,
			LibrarySection.Id.ARTISTS,
			LibrarySection.Id.GENRES,
			LibrarySection.Id.STATISTICS
		)

		val default = LibraryLayoutConfig(
			sections = DEFAULT_ORDER.map { LibrarySection(it, visible = it in DEFAULT_VISIBLE) },
			version = VERSION
		).normalize()

		fun decode(raw: String?): LibraryLayoutConfig {
			if (raw.isNullOrBlank()) return default
			val decoded = try {
				LayoutJson.json.decodeFromString<LibraryLayoutConfig>(raw)
			} catch (_: Exception) {
				return default
			}
			return decoded.normalize()
		}

		fun encode(config: LibraryLayoutConfig): String =
			LayoutJson.json.encodeToString(config.normalize())
	}
}

internal object LayoutJson {
	val json = kotlinx.serialization.json.Json {
		ignoreUnknownKeys = true
		encodeDefaults = true
	}
}
