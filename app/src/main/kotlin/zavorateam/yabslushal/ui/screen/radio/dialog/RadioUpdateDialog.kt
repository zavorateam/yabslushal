/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.radio.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zavorateam.yabslushal.R
import zavorateam.yabslushal.domain.model.DomainRadio
import zavorateam.yabslushal.ui.component.common.SegmentedListButton
import zavorateam.yabslushal.ui.component.common.SegmentedListButtonDefaults
import zavorateam.yabslushal.ui.component.dialog.FormDialog
import zavorateam.yabslushal.ui.core.UiState
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.outlined.Radio
import zavorateam.yabslushal.ui.screen.radio.viewmodel.RadioUpdateDialogViewModel

@Composable
fun RadioUpdateDialog(
	onDismissRequest: () -> Unit,
	radio: DomainRadio,
	onRefresh: () -> Unit
) {
	val viewModel = koinViewModel<RadioUpdateDialogViewModel>(
		parameters = { parametersOf(radio) }
	)
	val state by viewModel.uiState.collectAsStateWithLifecycle()

	LaunchedEffect(Unit) {
		viewModel.events.collect { event ->
			when (event) {
				is RadioUpdateDialogViewModel.Event.Dismiss -> {
					onDismissRequest()
					onRefresh()
				}
			}
		}
	}

	FormDialog(
		onDismissRequest = onDismissRequest,
		icon = { Icon(Icons.Outlined.Radio, null) },
		title = { Text(stringResource(R.string.title_update_radio)) },
		buttons = {
			SegmentedListButton(
				modifier = Modifier.fillMaxWidth(),
				onClick = viewModel::update,
				enabled = state !is UiState.Loading
					&& viewModel.name.text.isNotBlank()
					&& viewModel.streamUrl.text.isNotBlank(),
				shapes = SegmentedListButtonDefaults.shapes(index = 0, count = 2),
				colors = SegmentedListButtonDefaults.primaryColors()
			) {
				if (state is UiState.Loading) {
					CircularProgressIndicator(Modifier.size(20.dp))
				}
				Text(stringResource(R.string.action_ok))
			}
			SegmentedListButton(
				modifier = Modifier.fillMaxWidth(),
				onClick = onDismissRequest,
				enabled = state !is UiState.Loading,
				shapes = SegmentedListButtonDefaults.shapes(index = 1, count = 2)
			) {
				Text(stringResource(R.string.action_cancel))
			}
		},
		content = {
			(state as? UiState.Error)?.error?.let {
				SelectionContainer {
					Text("$it")
				}
			}
			val fieldModifier = Modifier.height(60.dp)
			val fieldLineLimits = TextFieldLineLimits.SingleLine
			Column(
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				TextField(
					modifier = fieldModifier,
					state = viewModel.name,
					label = { Text("${stringResource(R.string.option_radio_name)}*") },
					lineLimits = fieldLineLimits
				)
				TextField(
					modifier = fieldModifier,
					state = viewModel.streamUrl,
					label = { Text("${stringResource(R.string.option_radio_stream_url)}*") },
					lineLimits = fieldLineLimits
				)
				TextField(
					modifier = fieldModifier,
					state = viewModel.homepageUrl,
					label = { Text(stringResource(R.string.option_radio_homepage_url)) },
					lineLimits = fieldLineLimits
				)
			}
		}
	)
}
