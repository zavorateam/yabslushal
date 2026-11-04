/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.playlist.viewmodel

import androidx.compose.foundation.text.input.TextFieldState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import zavorateam.yabslushal.R
import zavorateam.yabslushal.database.dao.PlaylistDao
import zavorateam.yabslushal.database.mapper.toDomainModel
import zavorateam.yabslushal.database.mapper.toEntity
import zavorateam.yabslushal.domain.manager.session.SessionManager
import zavorateam.yabslushal.domain.manager.ui.SnackBarManager
import zavorateam.yabslushal.domain.model.DomainPlaylist
import zavorateam.yabslushal.ui.core.UiState
import zavorateam.yabslushal.ui.viewmodel.AsyncViewModel

class PlaylistCreateDialogViewModel(
	private val songIds: List<String>,
	private val playlistDao: PlaylistDao,
	private val sessionManager: SessionManager,
	private val snackBarManager: SnackBarManager
) : AsyncViewModel<Nothing?>(UiState.Success(null)) {
	private val _events = Channel<Event>()
	val events = _events.receiveAsFlow()

	val name = TextFieldState()

	fun create() {
		launch {
			setLoading()
			val playlist = sessionManager.api.createPlaylist(
				name = name.text.toString(),
				songIds = songIds
			)
			playlistDao.insertPlaylist(playlist.toEntity())
			snackBarManager.notify(R.string.notice_created_playlist, playlist.name)
			setSuccess(null)
			_events.send(
				Event.Dismiss(
					playlistDao.getPlaylistById(playlist.id)!!.toDomainModel()
				)
			)
		}
	}

	sealed class Event {
		data class Dismiss(val playlist: DomainPlaylist) : Event()
	}
}
