/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.collection.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import zavorateam.yabslushal.R
import zavorateam.yabslushal.database.entity.DownloadStatus
import zavorateam.yabslushal.domain.manager.session.DownloadManager
import zavorateam.yabslushal.domain.manager.ui.SnackBarManager
import zavorateam.yabslushal.domain.model.DomainSongCollection
import zavorateam.yabslushal.playback.MediaPlayer
import zavorateam.yabslushal.ui.component.dialog.DeleteDownloadTarget
import zavorateam.yabslushal.ui.component.dialog.rememberDownloadDeletionHandler
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.filled.Play
import zavorateam.yabslushal.ui.icons.outlined.Close
import zavorateam.yabslushal.ui.icons.outlined.Delete
import zavorateam.yabslushal.ui.icons.outlined.Download
import zavorateam.yabslushal.ui.icons.outlined.DownloadOff
import zavorateam.yabslushal.ui.icons.outlined.Shuffle
import zavorateam.yabslushal.ui.theme.defaultFont

@Composable
fun CollectionDetailScreenHeadingRowButtons(
	collection: DomainSongCollection
) {
	val player = koinInject<MediaPlayer>()
	val snackBarManager = koinInject<SnackBarManager>()
	val downloadManager = koinInject<DownloadManager>()
	val downloadDeletionHandler = rememberDownloadDeletionHandler()
	val scope = rememberCoroutineScope()

	val downloadStatus by downloadManager
		.getCollectionDownloadStatus(collection.songs.map { it.id })
		.collectAsState(initial = DownloadStatus.NOT_DOWNLOADED)

	Row(
		modifier = Modifier.padding(horizontal = 31.dp, vertical = 10.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(
			10.dp,
			alignment = Alignment.CenterHorizontally
		)
	) {
		val buttonShape = CircleShape
		val buttonHeight = 44.dp
		OutlinedButton(
			modifier = Modifier.size(width = 52.dp, height = buttonHeight),
			onClick = {
				player.shufflePlay(collection)
			},
			shape = buttonShape,
			contentPadding = PaddingValues(0.dp),
			enabled = collection.songs.isNotEmpty()
		) {
			Icon(
				Icons.Outlined.Shuffle,
				contentDescription = stringResource(R.string.action_shuffle),
				modifier = Modifier.size(24.dp)
			)
		}
		Button(
			modifier = Modifier
				.weight(1f)
				.height(buttonHeight),
			onClick = {
				player.playNow(collection)
			},
			shape = buttonShape,
			enabled = collection.songs.isNotEmpty()
		) {
			Icon(
				Icons.Filled.Play,
				null,
				modifier = Modifier
					.size(25.dp)
					.padding(end = 3.dp)
			)
			Text(
				stringResource(R.string.action_play),
				maxLines = 1,
				autoSize = TextAutoSize.StepBased(
					minFontSize = 1.sp,
					maxFontSize = 15.sp
				),
				fontWeight = FontWeight.SemiBold,
				fontFamily = defaultFont(round = 100f)
			)
		}
		OutlinedButton(
			modifier = Modifier.size(width = 52.dp, height = buttonHeight),
			onClick = {
				scope.launch {
					when (downloadStatus) {
						DownloadStatus.NOT_DOWNLOADED, DownloadStatus.FAILED -> {
							downloadManager.downloadCollection(collection.songs.map { it.id })
							snackBarManager.notify(R.string.notice_download_started)
						}

						DownloadStatus.DOWNLOADING -> {
							downloadManager.cancelCollectionDownload(collection.songs.map { it.id })
						}

						DownloadStatus.DOWNLOADED -> {
							downloadDeletionHandler.requestDelete(
								target = DeleteDownloadTarget.Collection.of(collection),
								notifySnackBar = true
							)
						}
					}
				}
			},
			shape = buttonShape,
			enabled = collection.songs.isNotEmpty() ||
				(downloadStatus == DownloadStatus.DOWNLOADED || downloadStatus == DownloadStatus.DOWNLOADING),
			contentPadding = PaddingValues(0.dp)
		) {
			when (downloadStatus) {
				DownloadStatus.DOWNLOADING -> {
					Box(contentAlignment = Alignment.Center) {
						CircularProgressIndicator(
							modifier = Modifier.size(24.dp),
							strokeWidth = 2.dp,
							color = MaterialTheme.colorScheme.primary
						)
						Icon(
							imageVector = Icons.Outlined.Close,
							contentDescription = "Cancel",
							modifier = Modifier.size(12.dp),
							tint = MaterialTheme.colorScheme.primary
						)
					}
				}

				DownloadStatus.DOWNLOADED -> {
					Icon(
						imageVector = Icons.Outlined.Delete,
						contentDescription = stringResource(R.string.action_delete_download),
						modifier = Modifier.size(24.dp),
						tint = MaterialTheme.colorScheme.primary
					)
				}

				DownloadStatus.FAILED -> {
					Icon(
						imageVector = Icons.Outlined.DownloadOff,
						contentDescription = stringResource(R.string.info_download_failed),
						modifier = Modifier.size(24.dp),
						tint = MaterialTheme.colorScheme.error
					)
				}

				else -> {
					Icon(
						imageVector = Icons.Outlined.Download,
						contentDescription = null,
						modifier = Modifier.size(24.dp)
					)
				}
			}
		}
	}
}
