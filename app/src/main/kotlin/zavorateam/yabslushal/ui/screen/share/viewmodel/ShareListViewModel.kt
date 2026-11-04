/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.share.viewmodel

import androidx.compose.foundation.lazy.grid.LazyGridState
import zavorateam.yabslushal.domain.manager.session.SessionManager
import zavorateam.yabslushal.domain.model.DomainShare
import zavorateam.yabslushal.domain.repository.ShareRepository
import zavorateam.yabslushal.ui.viewmodel.SelectableListViewModel

class ShareListViewModel(
	private val repository: ShareRepository,
	private val sessionManager: SessionManager
) : SelectableListViewModel<DomainShare>() {
	val gridState = LazyGridState()

	init {
		launch {
			sessionManager.sessionState.collect {
				refreshShares()
			}
		}
	}

	fun refreshShares() {
		launch {
			items = repository.getShares()
		}
	}
}
