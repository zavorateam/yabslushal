/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package paige.navic.ui.icons.filled

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import paige.navic.ui.icons.Icons

val Icons.Filled.Star: ImageVector
	get() {
		if (_Star != null) {
			return _Star!!
		}
		_Star = ImageVector.Builder(
			name = "Filled.Favorite",
			defaultWidth = 24.dp,
			defaultHeight = 24.dp,
			viewportWidth = 960f,
			viewportHeight = 960f
		).apply {
			path(fill = SolidColor(Color(0xFFE3E3E3))) {
				moveTo(480f, 840f)
				lineToRelative(-58f, -52f)
				quadToRelative(-103f, -94f, -170.5f, -163f)
				reflectiveQuadTo(144f, 482.5f)
				reflectiveQuadTo(100f, 375f)
				reflectiveQuadToRelative(-12f, -107f)
				quadToRelative(0f, -108f, 74f, -182f)
				reflectiveQuadToRelative(182f, -74f)
				quadToRelative(60f, 0f, 115f, 26f)
				reflectiveQuadToRelative(97f, 74f)
				quadToRelative(42f, -48f, 97f, -74f)
				reflectiveQuadToRelative(115f, -26f)
				quadToRelative(108f, 0f, 182f, 74f)
				reflectiveQuadToRelative(74f, 182f)
				quadToRelative(0f, 55f, -12f, 107f)
				reflectiveQuadToRelative(-44f, 142.5f)
				reflectiveQuadTo(808.5f, 625f)
				reflectiveQuadTo(638f, 788f)
				lineTo(480f, 840f)
				close()
			}
		}.build()

		return _Star!!
	}

@Suppress("ObjectPropertyName")
private var _Star: ImageVector? = null
