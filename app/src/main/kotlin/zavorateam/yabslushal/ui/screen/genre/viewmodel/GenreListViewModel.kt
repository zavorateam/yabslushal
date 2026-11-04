/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.genre.viewmodel

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import zavorateam.yabslushal.domain.manager.session.SessionManager
import zavorateam.yabslushal.domain.manager.session.SessionState
import zavorateam.yabslushal.domain.model.DomainGenre
import zavorateam.yabslushal.domain.repository.GenreRepository
import zavorateam.yabslushal.ui.viewmodel.AsyncViewModel

class GenreListViewModel(
	private val repository: GenreRepository,
	private val sessionManager: SessionManager
) : AsyncViewModel<Flow<PagingData<DomainGenre>>>() {

	val gridState = LazyGridState()

	init {
		viewModelScope.launch {
			sessionManager.sessionState.collect { if (it is SessionState.Authenticated) refreshGenres(false) }
		}
	}

	fun refreshGenres(fullRefresh: Boolean) {
		execute {
			repository.getGenres(fullRefresh).cachedIn(viewModelScope)
		}
	}
}
