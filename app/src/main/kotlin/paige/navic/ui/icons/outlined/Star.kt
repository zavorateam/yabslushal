/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package paige.navic.ui.icons.outlined

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import paige.navic.ui.icons.Icons

val Icons.Outlined.Star: ImageVector
	get() {
		if (_Star != null) {
			return _Star!!
		}
		_Star = ImageVector.Builder(
			name = "Outlined.FavoriteBorder",
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
				moveTo(480f, 758f)
				quadToRelative(136f, -124f, 203f, -190.5f)
				reflectiveQuadTo(750f, 442.5f)
				reflectiveQuadToRelative(18f, -94.5f)
				quadToRelative(0f, -75f, -50.5f, -125.5f)
				reflectiveQuadTo(592f, 172f)
				quadToRelative(-52f, 0f, -97f, 28.5f)
				reflectiveQuadTo(420f, 280f)
				lineTo(360f, 280f)
				quadToRelative(-30f, -51f, -75f, -79.5f)
				reflectiveQuadTo(188f, 172f)
				quadToRelative(-75f, 0f, -125.5f, 50.5f)
				reflectiveQuadTo(12f, 348f)
				quadToRelative(0f, 43f, 18f, 94.5f)
				reflectiveQuadToRelative(67f, 125f)
				reflectiveQuadTo(300f, 758f)
				lineTo(480f, 758f)
				close()
			}
		}.build()

		return _Star!!
	}

@Suppress("ObjectPropertyName")
private var _Star: ImageVector? = null
