/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package paige.navic.ui.screen.settings.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import org.koin.compose.koinInject
import paige.navic.domain.manager.runtime.PreferenceManager

/**
 * Input for the Yandex Music OAuth token used by the optional Yandex lyrics provider.
 *
 * The value is persisted (encrypted) as soon as the user types, so both the onboarding screen
 * and the settings screen share the very same storage. Typing "clear" via the trailing icon
 * removes the token.
 */
@Composable
fun YandexOAuthTokenField(
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	preferenceManager: PreferenceManager = koinInject()
) {
	val storedToken = preferenceManager.yandexOAuthToken
	val state = rememberTextFieldState(storedToken)

	// Keep the field in sync if the token changes elsewhere (e.g. cleared from settings).
	LaunchedEffect(storedToken) {
		if (state.text.toString() != storedToken) {
			state.edit { replace(0, length, storedToken) }
		}
	}

	LaunchedEffect(state) {
		snapshotFlow { state.text.toString() }
			.drop(1)
			.collect { value -> preferenceManager.yandexOAuthToken = value.trim() }
	}

	OutlinedSecureTextField(
		modifier = modifier
			.heightIn(min = 60.dp)
			.fillMaxWidth(),
		state = state,
		label = { Text("Яндекс OAuth") },
		supportingText = { Text("Токен вида y0_... для текстов песен из Яндекс Музыки") },
		enabled = enabled,
		keyboardOptions = KeyboardOptions(
			autoCorrectEnabled = false,
			keyboardType = KeyboardType.Password,
			imeAction = ImeAction.Done,
			showKeyboardOnFocus = true
		)
	)
}
