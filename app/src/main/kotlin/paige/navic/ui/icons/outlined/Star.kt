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
			name = "Outlined.Star",
			defaultWidth = 24.dp,
			defaultHeight = 24.dp,
			viewportWidth = 960f,
			viewportHeight = 960f
		).apply {
			path(fill = SolidColor(Color(0xFFE3E3E3))) {
				// Material Symbols "star" (outlined), centred on the 960x960 viewport
				moveTo(233f, 840f)
				lineTo(354f, 559f)
				lineTo(80f, 370f)
				lineTo(368f, 345f)
				lineTo(480f, 80f)
				lineTo(592f, 345f)
				lineTo(880f, 370f)
				lineTo(606f, 559f)
				lineTo(727f, 840f)
				lineTo(480f, 691f)
				close()
				// inner cut-out
				moveTo(354f, 673f)
				lineTo(480f, 597f)
				lineTo(606f, 673f)
				lineTo(573f, 529f)
				lineTo(684f, 433f)
				lineTo(538f, 420f)
				lineTo(480f, 284f)
				lineTo(422f, 420f)
				lineTo(276f, 433f)
				close()
			}
		}.build()

		return _Star!!
	}

@Suppress("ObjectPropertyName")
private var _Star: ImageVector? = null
