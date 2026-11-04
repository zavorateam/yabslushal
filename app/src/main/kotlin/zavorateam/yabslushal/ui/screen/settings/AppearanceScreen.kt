/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.koinInject
import zavorateam.yabslushal.R
import zavorateam.yabslushal.di.LocalNavStack
import zavorateam.yabslushal.di.LocalSizeClass
import zavorateam.yabslushal.domain.manager.runtime.PreferenceManager
import zavorateam.yabslushal.domain.model.settings.AnimationStyle
import zavorateam.yabslushal.domain.model.settings.MarqueeSpeed
import zavorateam.yabslushal.ui.component.common.SegmentedListItem
import zavorateam.yabslushal.ui.component.common.SegmentedListItemDefaults
import zavorateam.yabslushal.ui.component.layout.NestedTopBar
import zavorateam.yabslushal.ui.component.layout.NestedTopBarDefaults
import zavorateam.yabslushal.ui.navigation.Screen
import zavorateam.yabslushal.ui.screen.settings.component.SettingsChoiceItem
import zavorateam.yabslushal.ui.screen.settings.component.SettingsGroup
import zavorateam.yabslushal.ui.screen.settings.component.SettingsGroupDefaults
import zavorateam.yabslushal.ui.screen.settings.component.SettingsSliderItem
import zavorateam.yabslushal.ui.screen.settings.component.SettingsToggleItem
import zavorateam.yabslushal.ui.screen.settings.dialog.ArtworkShapeDialog
import zavorateam.yabslushal.ui.screen.settings.dialog.GridSizeDialog
import zavorateam.yabslushal.ui.screen.settings.dialog.GridSizePreview

@Composable
fun SettingsAppearanceScreen() {
	val preferenceManager = koinInject<PreferenceManager>()


	val backStack = LocalNavStack.current
	val sizeClass = LocalSizeClass.current
	val isCompact = sizeClass.widthSizeClass <= WindowWidthSizeClass.Compact
	val hideBack = sizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

	var showArtworkShapeDialog by rememberSaveable { mutableStateOf(false) }
	var showArtistImageShapeDialog by rememberSaveable { mutableStateOf(false) }
	var showGridSizeDialog by rememberSaveable { mutableStateOf(false) }

	Scaffold(
		topBar = {
			NestedTopBar(
				title = { Text(stringResource(R.string.title_appearance)) },
				navigationAction = {
					if (!hideBack) {
						NestedTopBarDefaults.NavigationAction()
					}
				}
			)
		}
	) { innerPadding ->
		CompositionLocalProvider(
			LocalMinimumInteractiveComponentSize provides 0.dp
		) {
			Column(
				modifier = Modifier
					.padding(innerPadding)
					.verticalScroll(rememberScrollState())
					.padding(horizontal = 16.dp),
				verticalArrangement = Arrangement.spacedBy(SettingsGroupDefaults.GapBetweenGroups)
			) {
				SettingsGroup {
					SegmentedListItem(
						shapes = SegmentedListItemDefaults.segmentedShapes(
							index = 0,
							count = 3
						),
						onClick = dropUnlessResumed {
							backStack.add(Screen.Settings.Fonts)
						},
						content = { Text(stringResource(R.string.title_choose_font)) },
						supportingContent = { Text(preferenceManager.font.displayName) }
					)
					SegmentedListItem(
						shapes = SegmentedListItemDefaults.segmentedShapes(
							index = 1,
							count = 3
						),
						onClick = dropUnlessResumed {
							backStack.add(Screen.Settings.Themes)
						},
						content = { Text(stringResource(R.string.option_choose_theme)) },
						supportingContent = { Text(stringResource(preferenceManager.theme.title)) }
					)
					SegmentedListItem(
						shapes = SegmentedListItemDefaults.segmentedShapes(
							index = 2,
							count = 3
						),
						onClick = dropUnlessResumed {
							backStack.add(Screen.Settings.AppIcon)
						},
						content = { Text(stringResource(R.string.option_choose_app_icon)) },
						supportingContent = { Text(preferenceManager.appIconVariant.name) }
					)
				}

				SettingsGroup(title = { Text(stringResource(R.string.title_layout)) }) {
					SegmentedListItem(
						shapes = SegmentedListItemDefaults.segmentedShapes(
							index = 0,
							count = 3
						),
						onClick = { showArtworkShapeDialog = true },
						content = { Text(stringResource(R.string.option_artwork_shape)) },
						supportingContent = { Text(preferenceManager.coverArtShape.name) },
						trailingContent = {
							val shape = preferenceManager.coverArtShape.decreasedShape
							Box(
								modifier = Modifier
									.size(48.dp)
									.clip(shape)
									.background(MaterialTheme.colorScheme.primaryContainer)
									.border(2.dp, MaterialTheme.colorScheme.primary, shape)
							)
						}
					)
					SegmentedListItem(
						shapes = SegmentedListItemDefaults.segmentedShapes(
							index = 1,
							count = 3
						),
						onClick = { showArtistImageShapeDialog = true },
						content = { Text(stringResource(R.string.option_artist_image_shape)) },
						supportingContent = { Text(preferenceManager.artistImageShape.name) },
						trailingContent = {
							val shape = preferenceManager.artistImageShape.decreasedShape
							Box(
								modifier = Modifier
									.size(48.dp)
									.clip(shape)
									.background(MaterialTheme.colorScheme.primaryContainer)
									.border(2.dp, MaterialTheme.colorScheme.primary, shape)
							)
						}
					)
					if (isCompact) {
						// preset sizes on phone
						SegmentedListItem(
							onClick = { showGridSizeDialog = true },
							content = { Text(stringResource(R.string.option_grid_items_per_row)) },
							supportingContent = { Text(preferenceManager.gridSize.label) },
							trailingContent = { GridSizePreview(preferenceManager.gridSize.value) },
							shapes = SegmentedListItemDefaults.segmentedShapes(
								index = 2,
								count = 3
							)
						)
					} else {
						// direct sizing slider on tablet
						SettingsSliderItem(
							value = preferenceManager.artGridItemSize,
							valueRange = 50f..500f,
							onValueChange = { preferenceManager.artGridItemSize = it },
							steps = 8,
							shapes = SegmentedListItemDefaults.segmentedShapes(
								index = 2,
								count = 3
							),
							content = { Text(stringResource(R.string.option_cover_art_size)) },
							trailingContent = { Text("${preferenceManager.artGridItemSize.toInt()}") }
						)
					}
				}

				SettingsGroup(title = { Text(stringResource(R.string.title_miscellaneous)) }) {
					SettingsToggleItem(
						content = { Text(stringResource(R.string.option_dynamic_theming)) },
						supportingContent = { Text(stringResource(R.string.subtitle_dynamic_theming)) },
						checked = preferenceManager.dynamicTheming,
						onCheckedChange = { preferenceManager.dynamicTheming = it },
						shapes = SegmentedListItemDefaults.segmentedShapes(index = 0, count = 6)
					)
					SettingsToggleItem(
						content = { Text(stringResource(R.string.option_alphabetical_scroll)) },
						checked = preferenceManager.alphabeticalScroll,
						onCheckedChange = { preferenceManager.alphabeticalScroll = it },
						shapes = SegmentedListItemDefaults.segmentedShapes(index = 1, count = 6)
					)
					SettingsToggleItem(
						content = { Text(stringResource(R.string.option_enable_predictive_back_animations)) },
						checked = preferenceManager.enablePredictiveBackAnimations,
						onCheckedChange = { preferenceManager.enablePredictiveBackAnimations = it },
						shapes = SegmentedListItemDefaults.segmentedShapes(index = 2, count = 6)
					)
					SettingsToggleItem(
						content = { Text(stringResource(R.string.option_enable_ratings)) },
						supportingContent = { Text(stringResource(R.string.subtitle_enable_ratings)) },
						checked = preferenceManager.enableRatings,
						onCheckedChange = { preferenceManager.enableRatings = it },
						shapes = SegmentedListItemDefaults.segmentedShapes(index = 3, count = 6)
					)
					SettingsChoiceItem(
						content = { Text(stringResource(R.string.option_use_marquee_text)) },
						choices = MarqueeSpeed.entries.toImmutableList(),
						selectedChoice = preferenceManager.marqueeSpeed,
						onChoiceSelected = { preferenceManager.marqueeSpeed = it },
						label = { it.name },
						shapes = SegmentedListItemDefaults.segmentedShapes(index = 4, count = 6)
					)
					SettingsChoiceItem(
						content = { Text(stringResource(R.string.option_animation_style)) },
						choices = AnimationStyle.entries.toImmutableList(),
						selectedChoice = preferenceManager.animationStyle,
						onChoiceSelected = { preferenceManager.animationStyle = it },
						label = { it.name },
						shapes = SegmentedListItemDefaults.segmentedShapes(index = 5, count = 7)
					)
					SettingsToggleItem(
						content = { Text("Отображать доп. разделы в Избранном") },
						supportingContent = { Text("Показывать часто прослушиваемые и рекомендации") },
						checked = preferenceManager.showExtraInFavorites,
						onCheckedChange = { preferenceManager.showExtraInFavorites = it },
						shapes = SegmentedListItemDefaults.segmentedShapes(index = 6, count = 7)
					)
				}
			}
		}
	}

	ArtworkShapeDialog(
		title = { Text(stringResource(R.string.option_artwork_shape)) },
		selection = preferenceManager.coverArtShape,
		onSelect = { preferenceManager.coverArtShape = it },
		presented = showArtworkShapeDialog,
		onDismissRequest = { showArtworkShapeDialog = false }
	)
	ArtworkShapeDialog(
		title = { Text(stringResource(R.string.option_artist_image_shape)) },
		selection = preferenceManager.artistImageShape,
		onSelect = { preferenceManager.artistImageShape = it },
		presented = showArtistImageShapeDialog,
		onDismissRequest = { showArtistImageShapeDialog = false }
	)
	GridSizeDialog(
		presented = showGridSizeDialog,
		onDismissRequest = { showGridSizeDialog = false }
	)
}
