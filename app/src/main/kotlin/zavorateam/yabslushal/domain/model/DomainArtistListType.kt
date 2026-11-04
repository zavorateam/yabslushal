/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.model

import androidx.compose.runtime.Immutable
import zavorateam.yabslushal.R

@Immutable
enum class DomainArtistListType(val displayName: Int) {
	AlphabeticalByName(R.string.option_sort_alphabetical_by_name),
	Random(R.string.option_sort_random)
}
