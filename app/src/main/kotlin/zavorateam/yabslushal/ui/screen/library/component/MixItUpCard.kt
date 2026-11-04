/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.library.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import zavorateam.yabslushal.R
import zavorateam.yabslushal.ui.theme.defaultFont

/**
 * Entry point for the Mix It Up queue engine.
 *
 * The card replaces the old "random albums" quick action: instead of opening an album grid sorted
 * at random, it starts playback from a locally generated mix that respects the user's habits and
 * the two Mix It Up parameters.
 */
fun LazyGridScope.mixItUpCard(
	icon: ImageVector,
	onStart: () -> Unit
) {
	item(span = { GridItemSpan(maxLineSpan) }) {
		Button(
			modifier = Modifier
				.fillMaxWidth()
				.height(52.dp)
				.padding(horizontal = 16.dp),
			contentPadding = PaddingValues(horizontal = 16.dp),
			elevation = null,
			shapes = ButtonDefaults.shapes(
				shape = MaterialTheme.shapes.small,
				pressedShape = MaterialTheme.shapes.extraSmall
			),
			colors = ButtonDefaults.buttonColors(
				containerColor = MaterialTheme.colorScheme.primaryContainer,
				contentColor = MaterialTheme.colorScheme.onPrimaryContainer
			),
			onClick = onStart
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				Icon(icon, contentDescription = null)
				Spacer(Modifier.width(10.dp))
				Column(
					modifier = Modifier.weight(1f),
					verticalArrangement = Arrangement.Center
				) {
					Text(
						stringResource(R.string.title_mix_it_up),
						fontFamily = defaultFont(100, round = 100f),
						style = MaterialTheme.typography.titleSmall
					)
					Text(
						stringResource(R.string.subtitle_mix_it_up),
						fontFamily = defaultFont(100, round = 100f),
						style = MaterialTheme.typography.bodySmall,
						maxLines = 1
					)
				}
			}
		}
	}
}
