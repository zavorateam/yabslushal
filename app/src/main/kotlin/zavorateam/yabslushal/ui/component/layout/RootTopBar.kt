/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.component.layout

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import org.koin.compose.viewmodel.koinViewModel
import zavorateam.yabslushal.R
import zavorateam.yabslushal.di.LocalNavStack
import zavorateam.yabslushal.domain.model.settings.NavbarConfig
import zavorateam.yabslushal.domain.model.settings.NavbarTab
import zavorateam.yabslushal.ui.component.common.TooltipBox
import zavorateam.yabslushal.ui.component.sheet.AccountSheet
import zavorateam.yabslushal.ui.core.UiState
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.filled.Settings
import zavorateam.yabslushal.ui.icons.outlined.AccountCircle
import zavorateam.yabslushal.ui.icons.outlined.Search
import zavorateam.yabslushal.ui.navigation.Screen
import zavorateam.yabslushal.ui.screen.settings.viewmodel.NavtabsViewModel

@Composable
fun RootTopBar(
	title: @Composable () -> Unit,
	scrollBehavior: TopAppBarScrollBehavior,
	actions: @Composable RowScope.() -> Unit = {},
) {
	val navViewModel = koinViewModel<NavtabsViewModel>()
	val navState by navViewModel.state.collectAsStateWithLifecycle()
	val navConfig = (navState as? UiState.Success)?.data

	MediumFlexibleTopAppBar(
		title = {
			CompositionLocalProvider(
				LocalTextStyle provides when (LocalTextStyle.current) {
					MaterialTheme.typography.headlineMedium -> MaterialTheme.typography.headlineSmall
					else -> MaterialTheme.typography.titleLarge
				}
			) {
				title()
			}
		},
		actions = {
			actions()
			Actions(navConfig = navConfig)
		},
		scrollBehavior = scrollBehavior,
		colors = TopAppBarDefaults.topAppBarColors(
			scrolledContainerColor = MaterialTheme.colorScheme.surface
		),
	)
}

@Composable
private fun Actions(
	navConfig: NavbarConfig?,
) {
	val backStack = LocalNavStack.current

	val isSearchEnabled = navConfig?.tabs?.any {
		it.id == NavbarTab.Id.SEARCH && it.visible
	} == true

	var accountSheetOpen by rememberSaveable { mutableStateOf(false) }

	if (!isSearchEnabled) {
		TooltipBox(stringResource(R.string.title_search)) {
			IconButton(
				onClick = dropUnlessResumed {
					backStack.add(Screen.Search(nested = true))
				}
			) {
				Icon(
					imageVector = Icons.Outlined.Search,
					contentDescription = stringResource(R.string.title_search)
				)
			}
		}
	}

	TooltipBox(stringResource(R.string.title_settings)) {
		IconButton(onClick = dropUnlessResumed {
			backStack.add(Screen.Settings.Root)
		}) {
			Icon(
				imageVector = Icons.Filled.Settings,
				contentDescription = stringResource(R.string.title_settings)
			)
		}
	}

	TooltipBox(stringResource(R.string.title_account)) {
		IconButton(onClick = {
			accountSheetOpen = true
		}) {
			Icon(
				imageVector = Icons.Outlined.AccountCircle,
				contentDescription = stringResource(R.string.title_account)
			)
		}
	}

	if (accountSheetOpen) {
		AccountSheet(onDismissRequest = { accountSheetOpen = false })
	}
}
