/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.radio.viewmodel

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import kotlinx.coroutines.launch
import zavorateam.yabslushal.domain.manager.session.SessionManager
import zavorateam.yabslushal.domain.manager.session.SessionState
import zavorateam.yabslushal.domain.model.DomainRadio
import zavorateam.yabslushal.domain.repository.RadioRepository
import zavorateam.yabslushal.ui.viewmodel.SelectableViewModel

class RadioListViewModel(
	private val repository: RadioRepository,
	private val sessionManager: SessionManager
) : SelectableViewModel<DomainRadio>() {
	val gridState = LazyGridState()

	init {
		viewModelScope.launch {
			sessionManager.sessionState.collect { if (it is SessionState.Authenticated) refreshRadios(false) }
		}
	}

	fun refreshRadios(fullRefresh: Boolean) = launch {
		items = repository.getRadios(fullRefresh).cachedIn(viewModelScope)
	}
}
