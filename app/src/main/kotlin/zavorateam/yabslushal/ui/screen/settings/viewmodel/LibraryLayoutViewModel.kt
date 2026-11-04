/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.settings.viewmodel

import androidx.lifecycle.ViewModel
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.model.settings.LibraryLayoutConfig
import zavorateam.yabslushal.domain.model.settings.LibrarySection

/**
 * Drives the library home screen editor. Every mutation goes through [LibraryLayoutConfig]'s
 * pure helpers and is persisted immediately, so the library screen picks the new layout up on
 * the next recomposition without any explicit "apply" step.
 */
class LibraryLayoutViewModel(
	private val preferenceManager: PreferenceManager
) : ViewModel() {
	var config: LibraryLayoutConfig
		get() = preferenceManager.libraryLayout.normalize()
		private set(value) {
			preferenceManager.libraryLayout = value.normalize()
		}

	fun move(
		from: Int,
		to: Int
	) {
		config = config.withMoved(from, to)
	}

	fun toggleVisibility(id: LibrarySection.Id) {
		config = config.withToggled(id)
	}

	fun reset() {
		config = LibraryLayoutConfig.default
	}
}
