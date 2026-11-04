/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.icons.filled

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import zavorateam.yabslushal.ui.icons.Icons

val Icons.Filled.Heart: ImageVector
	get() {
		if (_Heart != null) {
			return _Heart!!
		}
		_Heart = ImageVector.Builder(
			name = "Filled.Heart",
			defaultWidth = 24.dp,
			defaultHeight = 24.dp,
			viewportWidth = 960f,
			viewportHeight = 960f
		).apply {
			path(fill = SolidColor(Color(0xFFE3E3E3))) {
				// Material Symbols Rounded "favorite" (filled), centred on the 960x960 viewport
				moveTo(480f, 833.5f)
				quadTo(466f, 833.5f, 451.5f, 828.5f)
				reflectiveQuadTo(426f, 812.5f)
				lineTo(357f, 749.5f)
				quadTo(251f, 652.5f, 165.5f, 557f)
				reflectiveQuadTo(80f, 346.5f)
				quadTo(80f, 252.5f, 143f, 189.5f)
				reflectiveQuadTo(300f, 126.5f)
				quadTo(353f, 126.5f, 400f, 149f)
				reflectiveQuadTo(480f, 210.5f)
				quadTo(513f, 171.5f, 560f, 149f)
				reflectiveQuadTo(660f, 126.5f)
				quadTo(754f, 126.5f, 817f, 189.5f)
				reflectiveQuadTo(880f, 346.5f)
				quadTo(880f, 461.5f, 795f, 557.5f)
				reflectiveQuadTo(602f, 750.5f)
				lineTo(534f, 812.5f)
				quadTo(523f, 823.5f, 508.5f, 828.5f)
				reflectiveQuadTo(480f, 833.5f)
				close()
			}
		}.build()

		return _Heart!!
	}

@Suppress("ObjectPropertyName")
private var _Heart: ImageVector? = null
