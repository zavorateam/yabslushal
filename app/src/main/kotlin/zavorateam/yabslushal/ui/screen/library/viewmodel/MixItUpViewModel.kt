/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zavorateam.yabslushal.domain.mix.MixDuration
import zavorateam.yabslushal.domain.mix.MixFilter
import zavorateam.yabslushal.domain.mix.MixTempo
import zavorateam.yabslushal.domain.repository.SongRepository

/**
 * State of the Mix It Up dialog.
 *
 * @property presented whether the dialog is on screen
 * @property genres every genre the local library knows about
 * @property moods every mood the local library knows about
 * @property filter the selection being edited, not yet started
 */
data class MixItUpUiState(
	val presented: Boolean = false,
	val genres: List<String> = emptyList(),
	val moods: List<String> = emptyList(),
	val filter: MixFilter = MixFilter.NONE,
	val duration: MixDuration = MixDuration.Any
)

/**
 * Backs the Mix It Up dialog: loads the selectable values from the local library and keeps the
 * pending selection until the user starts the mix.
 */
class MixItUpViewModel(
	private val songRepository: SongRepository
) : ViewModel() {

	private val _state = MutableStateFlow(MixItUpUiState())
	val state: StateFlow<MixItUpUiState> = _state.asStateFlow()

	fun open() {
		_state.update { it.copy(presented = true) }
		viewModelScope.launch {
			val genres = songRepository.getGenres()
			val moods = songRepository.getMoods()
			_state.update { it.copy(genres = genres, moods = moods) }
		}
	}

	fun dismiss() {
		_state.update { it.copy(presented = false) }
	}

	/** Drops every narrowing: the mix then behaves like the plain engine. */
	fun selectNothing() {
		_state.update { it.copy(filter = MixFilter.NONE, duration = MixDuration.Any) }
	}

	fun toggleGenre(genre: String) {
		_state.update { current ->
			val genres = current.filter.genres.toMutableSet()
			if (!genres.add(genre)) genres.remove(genre)
			current.copy(filter = current.filter.copy(genres = genres))
		}
	}

	fun toggleMood(mood: String) {
		_state.update { current ->
			val moods = current.filter.moods.toMutableSet()
			if (!moods.add(mood)) moods.remove(mood)
			current.copy(filter = current.filter.copy(moods = moods))
		}
	}

	fun setDuration(duration: MixDuration) {
		_state.update { current ->
			current.copy(
				duration = duration,
				filter = current.filter.copy(
					minDurationSeconds = duration.minSeconds,
					maxDurationSeconds = duration.maxSeconds
				)
			)
		}
	}

	fun setTempo(tempo: MixTempo) {
		_state.update { current -> current.copy(filter = current.filter.copy(tempo = tempo)) }
	}

	/** Builds the mix around an artist instead of a single track. */
	fun setSeedArtist(artist: String?) {
		_state.update { current ->
			current.copy(
				filter = current.filter.copy(
					seedArtist = artist,
					seedSongId = if (artist != null) null else current.filter.seedSongId
				)
			)
		}
	}

	fun setSeedSong(songId: String?) {
		_state.update { current ->
			current.copy(
				filter = current.filter.copy(
					seedSongId = songId,
					seedArtist = if (songId != null) null else current.filter.seedArtist
				)
			)
		}
	}
}
