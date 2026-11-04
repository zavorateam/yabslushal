/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.nowPlaying.component.controls

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.koinInject
import zavorateam.yabslushal.R
import zavorateam.yabslushal.di.LocalNavStack
import zavorateam.yabslushal.domain.model.DomainPlaylist
import zavorateam.yabslushal.playback.MediaPlayer
import zavorateam.yabslushal.ui.component.dialog.ShareDialog
import zavorateam.yabslushal.ui.component.sheet.SleepTimerSheet
import zavorateam.yabslushal.ui.component.sheet.SongSheet
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.outlined.MoreHoriz
import zavorateam.yabslushal.ui.navigation.Screen
import zavorateam.yabslushal.ui.screen.playlist.dialog.PlaylistUpdateDialog
import zavorateam.yabslushal.ui.theme.YabslushalTheme
import zavorateam.yabslushal.ui.util.rememberColorSchemeFromCoverArt

@Composable
fun NowPlayingMoreButton(
	songRating: Int,
	onSetSongRating: (Int) -> Unit
) {
	val backStack = LocalNavStack.current
	val player = koinInject<MediaPlayer>()
	val playerState by player.uiState.collectAsStateWithLifecycle()
	val song = playerState.currentSong
	var expanded by remember { mutableStateOf(false) }
	var sleepTimerSheetShown by rememberSaveable { mutableStateOf(false) }
	var playlistDialogShown by rememberSaveable { mutableStateOf(false) }
	var shareId by remember { mutableStateOf<String?>(null) }
	val colorScheme = rememberColorSchemeFromCoverArt(song?.coverArtId)

	IconButton(
		onClick = {
			expanded = true
		},
		colors = IconButtonDefaults.filledTonalIconButtonColors(),
		modifier = Modifier.size(32.dp),
		enabled = song != null
	) {
		Icon(
			imageVector = Icons.Outlined.MoreHoriz,
			contentDescription = stringResource(R.string.action_more)
		)
	}

	if (expanded && song != null) {
		YabslushalTheme(colorScheme) {
			SongSheet(
				onDismissRequest = { expanded = false },
				song = song,
				isPlaylist = playerState.currentCollection is DomainPlaylist,
				onViewAlbum = dropUnlessResumed {
					playerState.currentCollection?.let { collection ->
						backStack.remove(Screen.NowPlaying)
						backStack.add(Screen.CollectionDetail(collection.id, ""))
					}
				},
				onViewArtist = dropUnlessResumed {
					backStack.remove(Screen.NowPlaying)
					backStack.add(Screen.ArtistDetail(song.artistId))
				},
				onShare = {
					shareId = song.id
				},
				onAddToPlaylist = {
					playlistDialogShown = true
				},
				onTrackInfo = dropUnlessResumed {
					expanded = false
					backStack.add(Screen.SongDetailSheet(songId = song.id, coverArtId = song.coverArtId))
				},
				rating = songRating,
				onSetRating = onSetSongRating,
				showSleepTimer = true,
				onSleepTimer = {
					expanded = false
					sleepTimerSheetShown = true
				},
				showPlaybackSpeed = true,
				onPlaybackSpeed = {
					expanded = false
					backStack.add(Screen.PlaybackSpeed)
				}
			)
		}
	}

	if (sleepTimerSheetShown) {
		YabslushalTheme(colorScheme) {
			SleepTimerSheet(
				onDismissRequest = { sleepTimerSheetShown = false }
			)
		}
	}

	if (playlistDialogShown && song != null) {
		YabslushalTheme(colorScheme) {
			PlaylistUpdateDialog(
				songIds = persistentListOf(song.id),
				onDismissRequest = { playlistDialogShown = false }
			)
		}
	}

	YabslushalTheme(colorScheme) {
		ShareDialog(
			id = shareId,
			onDismissRequest = { shareId = null }
		)
	}
}
