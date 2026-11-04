/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.library.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import zavorateam.yabslushal.R
import zavorateam.yabslushal.domain.mix.MixDuration
import zavorateam.yabslushal.domain.mix.MixTempo
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.outlined.Refresh
import zavorateam.yabslushal.ui.screen.library.viewmodel.MixItUpUiState

/**
 * Filter sheet for Mix It Up.
 *
 * Everything is optional: pressing "не важно" clears the selection and starts a mix from the whole
 * library, while picking genres, moods, a duration, a tempo or an artist narrows the pool first.
 */
@Composable
fun MixItUpDialog(
	state: MixItUpUiState,
	onToggleGenre: (String) -> Unit,
	onToggleMood: (String) -> Unit,
	onDurationChange: (MixDuration) -> Unit,
	onTempoChange: (MixTempo) -> Unit,
	onClear: () -> Unit,
	onStart: () -> Unit,
	onDismiss: () -> Unit
) {
	if (!state.presented) return

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(R.string.title_mix_it_up)) },
		text = {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.heightIn(max = 460.dp)
					.verticalScroll(rememberScrollState()),
				verticalArrangement = Arrangement.spacedBy(3.dp)
			) {
				Text(
					stringResource(R.string.subtitle_mix_it_up_dialog),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)

				if (state.genres.isNotEmpty()) {
					SectionTitle(stringResource(R.string.option_mix_it_up_genres))
					FlowRow(
						horizontalArrangement = Arrangement.spacedBy(4.dp),
						verticalArrangement = Arrangement.spacedBy(2.dp)
					) {
						state.genres.forEach { genre ->
							FilterChip(
								selected = genre in state.filter.genres,
								onClick = { onToggleGenre(genre) },
								label = { Text(genre) }
							)
						}
					}
				}

				if (state.moods.isNotEmpty()) {
					SectionTitle(stringResource(R.string.option_mix_it_up_moods))
					FlowRow(
						horizontalArrangement = Arrangement.spacedBy(4.dp),
						verticalArrangement = Arrangement.spacedBy(2.dp)
					) {
						state.moods.forEach { mood ->
							FilterChip(
								selected = mood in state.filter.moods,
								onClick = { onToggleMood(mood) },
								label = { Text(mood) }
							)
						}
					}
				}

				// A segmented row could not fit four russian labels: it clipped the text and
				// pushed the row wider than the dialog. A two column grid of equal cells keeps
				// every label on one line.
				SectionTitle(stringResource(R.string.option_mix_it_up_duration))
				ChoiceGrid(
					options = MixDuration.entries,
					selected = state.duration,
					label = { stringResource(it.displayName) },
					onSelect = onDurationChange
				)

				// Without tempo tags every option would match nothing, so the row is hidden
				// instead of offering a choice that can only fail.
				if (state.tempoAvailable) {
					SectionTitle(stringResource(R.string.option_mix_it_up_tempo))
					ChoiceGrid(
						options = MixTempo.entries,
						selected = state.filter.tempo,
						label = { stringResource(it.displayName) },
						onSelect = onTempoChange
					)
				}

				if (state.filter.seedArtist != null) {
					Row(modifier = Modifier.fillMaxWidth()) {
						Text(
							stringResource(R.string.option_mix_it_up_seed_artist) + ": " +
								state.filter.seedArtist,
							style = MaterialTheme.typography.bodySmall
						)
					}
				}
			}
		},
		// The real actions are rendered together in dismissButton, so AlertDialog gets no
		// separate confirm button: that keeps one row that is guaranteed to fit.
		confirmButton = {},
		// Cancel and start are the two real actions. "I don't care" clears every filter and sits
		// to their left as a separate, equally reachable shortcut.
		// Three actions do not fit on one line at this dialog width: the long "Мне всё равно"
		// label alone takes a quarter of the screen. It is therefore rendered as a single
		// icon button on the far left, whose meaning is spelled out below the dialog actions.
		dismissButton = {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				IconButton(onClick = onClear) {
					Icon(
						Icons.Outlined.Refresh,
						contentDescription = stringResource(R.string.option_mix_it_up_dont_care)
					)
				}
				Row(verticalAlignment = Alignment.CenterVertically) {
					TextButton(onClick = onDismiss) {
						Text(stringResource(R.string.action_cancel))
					}
					Button(onClick = onStart) {
						Text(stringResource(R.string.action_start_mix))
					}
				}
			}
		}
	)
}

/**
 * Uniform choice grid: equal cells, one line per label, so long localized strings never wrap
 * into the next cell or get clipped.
 */
@Composable
private fun <T> ChoiceGrid(
	options: List<T>,
	selected: T,
	label: @Composable (T) -> String,
	onSelect: (T) -> Unit
) {
	Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
		options.chunked(2).forEach { row ->
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(4.dp)
			) {
				row.forEach { option ->
					FilterChip(
						selected = option == selected,
						onClick = { onSelect(option) },
						label = {
							Text(
								label(option),
								maxLines = 1,
								modifier = Modifier.fillMaxWidth(),
								textAlign = TextAlign.Center
							)
						},
						modifier = Modifier.weight(1f)
					)
				}
				// Keep the last row aligned with the grid when the option count is odd.
				if (row.size == 1) Spacer(Modifier.weight(1f))
			}
		}
	}
}

@Composable
private fun SectionTitle(text: String) {
	Text(
		text,
		style = MaterialTheme.typography.titleSmall,
		modifier = Modifier.padding(top = 1.dp)
	)
}

private val MixDuration.displayName: Int
	get() = when (this) {
		MixDuration.Any -> R.string.option_mix_duration_any
		MixDuration.Short -> R.string.option_mix_duration_short
		MixDuration.Medium -> R.string.option_mix_duration_medium
		MixDuration.Long -> R.string.option_mix_duration_long
	}

private val MixTempo.displayName: Int
	get() = when (this) {
		MixTempo.Any -> R.string.option_mix_tempo_any
		MixTempo.Slow -> R.string.option_mix_tempo_slow
		MixTempo.Medium -> R.string.option_mix_tempo_medium
		MixTempo.Fast -> R.string.option_mix_tempo_fast
	}
