/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.icons.outlined

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import zavorateam.yabslushal.ui.icons.Icons

val Icons.Outlined.Heart: ImageVector
	get() {
		if (_Heart != null) {
			return _Heart!!
		}
		_Heart = ImageVector.Builder(
			name = "Outlined.Heart",
			defaultWidth = 24.dp,
			defaultHeight = 24.dp,
			viewportWidth = 960f,
			viewportHeight = 960f
		).apply {
			path(fill = SolidColor(Color(0xFFE3E3E3))) {
				// Material Symbols Rounded "favorite" (outlined), centred on the 960x960 viewport
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
				// inner cut-out
				moveTo(442f, 290.5f)
				quadTo(413f, 249.5f, 380f, 228f)
				reflectiveQuadTo(300f, 206.5f)
				quadTo(240f, 206.5f, 200f, 246.5f)
				reflectiveQuadTo(160f, 346.5f)
				quadTo(160f, 398.5f, 197f, 457f)
				reflectiveQuadTo(285.5f, 570.5f)
				quadTo(337f, 625.5f, 391.5f, 673.5f)
				reflectiveQuadTo(480f, 752.5f)
				quadTo(514f, 721.5f, 568.5f, 673.5f)
				reflectiveQuadTo(674.5f, 570.5f)
				quadTo(726f, 515.5f, 763f, 457f)
				reflectiveQuadTo(800f, 346.5f)
				quadTo(800f, 286.5f, 760f, 246.5f)
				reflectiveQuadTo(660f, 206.5f)
				quadTo(613f, 206.5f, 580f, 228f)
				reflectiveQuadTo(518f, 290.5f)
				quadTo(511f, 300.5f, 501f, 305.5f)
				reflectiveQuadTo(480f, 310.5f)
				quadTo(469f, 310.5f, 459f, 305.5f)
				reflectiveQuadTo(442f, 290.5f)
				close()
			}
		}.build()

		return _Heart!!
	}

@Suppress("ObjectPropertyName")
private var _Heart: ImageVector? = null
