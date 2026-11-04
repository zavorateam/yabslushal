/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.collection.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.collections.immutable.toPersistentList
import zavorateam.yabslushal.R
import zavorateam.yabslushal.di.LocalNavStack
import zavorateam.yabslushal.domain.model.DomainAlbum
import zavorateam.yabslushal.domain.model.DomainAlbumInfo
import zavorateam.yabslushal.domain.model.DomainSongCollection
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.outlined.MoreVert
import zavorateam.yabslushal.ui.component.layout.NestedTopBar
import zavorateam.yabslushal.ui.component.layout.TopBarButton
import zavorateam.yabslushal.ui.component.sheet.CollectionSheet
import zavorateam.yabslushal.ui.navigation.Screen
import zavorateam.yabslushal.ui.screen.playlist.dialog.PlaylistUpdateDialog

@Composable
fun CollectionDetailScreenTopBar(
	collection: DomainSongCollection?,
	albumInfo: DomainAlbumInfo?,
	titleAlpha: Float,
	onSetShareId: (shareId: String?) -> Unit,
	onPlayNext: () -> Unit,
	onAddToQueue: () -> Unit,
	rating: Int?,
	onSetRating: ((Int) -> Unit)?,
	starred: Boolean?,
	onSetStarred: ((Boolean) -> Unit)? = null,
	refreshCollection: () -> Unit
) {
	var playlistDialogShown by rememberSaveable { mutableStateOf(false) }
	val backStack = LocalNavStack.current

	NestedTopBar(
		title = {
			Text(
				text = collection?.name.orEmpty(),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.alpha(titleAlpha)
			)
		},
		actions = {
			Box {
				var expanded by rememberSaveable { mutableStateOf(false) }
				TopBarButton(onClick = {
					expanded = true
					refreshCollection()
				}) {
					Icon(
						Icons.Outlined.MoreVert,
						stringResource(R.string.action_more)
					)
				}
				if (expanded && collection != null) {
					CollectionSheet(
						onDismissRequest = { expanded = false },
						collection = collection,
						albumInfo = albumInfo,
						onShare = { onSetShareId(collection.id) },
						onPlayNext = onPlayNext,
						onAddToQueue = onAddToQueue,
						onAddAllToPlaylist = { playlistDialogShown = true },
						onViewArtist =
							if (collection is DomainAlbum)
								dropUnlessResumed { backStack.add(Screen.ArtistDetail(collection.artistId)) }
							else null,
						rating = rating,
						onSetRating = onSetRating,
						starred = starred,
						onSetStarred = if (onSetStarred != null && starred != null) {
							{ onSetStarred(!starred) }
						} else null
					)
				}
			}
		}
	)

	if (playlistDialogShown) {
		PlaylistUpdateDialog(
			songIds = collection?.songs.orEmpty().map { it.id }.toPersistentList(),
			onDismissRequest = { playlistDialogShown = false }
		)
	}
}
