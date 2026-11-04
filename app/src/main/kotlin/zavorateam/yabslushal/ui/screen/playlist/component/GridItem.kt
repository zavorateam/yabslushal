/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.playlist.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import zavorateam.yabslushal.R
import zavorateam.yabslushal.di.LocalNavStack
import zavorateam.yabslushal.domain.model.DomainPlaylist
import zavorateam.yabslushal.ui.component.layout.ArtGridItem
import zavorateam.yabslushal.ui.component.sheet.CollectionSheet
import zavorateam.yabslushal.ui.navigation.Screen
import zavorateam.yabslushal.ui.screen.playlist.dialog.PlaylistUpdateDialog

@Composable
fun PlaylistListScreenGridItem(
	modifier: Modifier = Modifier,
	tab: String,
	playlist: DomainPlaylist,
	selected: Boolean,
	onPlayNext: () -> Unit,
	onAddToQueue: () -> Unit,
	onSelect: () -> Unit,
	onDeselect: () -> Unit,
	onSetShareId: (String) -> Unit,
	onSetDeletionId: (String) -> Unit
) {
	val backStack = LocalNavStack.current
	val scope = rememberCoroutineScope()

	var playlistDialogShown by rememberSaveable { mutableStateOf(false) }

	Box(modifier) {
		ArtGridItem(
			onClick = dropUnlessResumed {
				scope.launch {
					backStack.add(Screen.CollectionDetail(playlist.id, tab))
				}
			},
			onLongClick = onSelect,
			coverArtId = playlist.coverArtId,
			title = playlist.name ?: "[unknown playlist]",
			subtitle = buildString {
				append(
					pluralStringResource(
						R.plurals.count_songs,
						playlist.songCount,
						playlist.songCount
					)
				)
				playlist.comment?.let {
					append("\n${playlist.comment}\n")
				}
			},
			id = playlist.id,
			tab = tab
		)
		if (selected) {
			CollectionSheet(
				onDismissRequest = onDeselect,
				collection = playlist,
				onShare = { onSetShareId(playlist.id) },
				onDelete = { onSetDeletionId(playlist.id) },
				onPlayNext = onPlayNext,
				onAddToQueue = onAddToQueue,
				onAddAllToPlaylist = { playlistDialogShown = true }
			)
		}

		if (playlistDialogShown) {
			PlaylistUpdateDialog(
				songIds = playlist.songs.map { it.id }.toPersistentList(),
				onDismissRequest = { playlistDialogShown = false }
			)
		}
	}
}
