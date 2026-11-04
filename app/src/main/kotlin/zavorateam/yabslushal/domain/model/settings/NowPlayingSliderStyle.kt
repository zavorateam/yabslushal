/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.model.settings

import zavorateam.yabslushal.R

enum class NowPlayingSliderStyle(val displayName: Int) {
	Flat(R.string.option_now_playing_slider_style_flat),
	Squiggly(R.string.option_now_playing_slider_style_squiggly),
	Slim(R.string.option_now_playing_slider_style_slim),
	Yoyo(R.string.option_now_playing_slider_style_yoyo)
}
