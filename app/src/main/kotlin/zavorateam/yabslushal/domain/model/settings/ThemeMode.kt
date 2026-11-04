/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.model.settings

import zavorateam.yabslushal.R
import zavorateam.yabslushal.util.PlatformSupport


enum class ThemeMode(val title: Int) {
	System(R.string.theme_mode_system),
	Dark(R.string.theme_mode_dark),
	Light(R.string.theme_mode_light);

	companion object {
		fun getDefaultMode(): ThemeMode {
			return if (!PlatformSupport.SUPPORTS_DARK_THEME) {
				Light
			} else {
				System
			}
		}
	}
}
