/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.domain.model.settings

import androidx.compose.ui.graphics.vector.ImageVector
import zavorateam.yabslushal.R
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.outlined.Grid
import zavorateam.yabslushal.ui.icons.outlined.List

enum class ListViewMode(val displayName: Int, val icon: ImageVector) {
	Grid(R.string.option_list_view_mode_grid, Icons.Outlined.Grid),
	List(R.string.option_list_view_mode_list, Icons.Outlined.List)
}
