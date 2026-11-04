/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import zavorateam.yabslushal.R
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.filled.Album
import zavorateam.yabslushal.ui.icons.filled.Artist
import zavorateam.yabslushal.ui.icons.filled.Genre
import zavorateam.yabslushal.ui.icons.filled.LibraryMusic
import zavorateam.yabslushal.ui.icons.filled.Radio
import zavorateam.yabslushal.ui.icons.outlined.Album
import zavorateam.yabslushal.ui.icons.outlined.Artist
import zavorateam.yabslushal.ui.icons.outlined.Genre
import zavorateam.yabslushal.ui.icons.outlined.LibraryMusic
import zavorateam.yabslushal.ui.icons.outlined.Note
import zavorateam.yabslushal.ui.icons.outlined.PlaylistPlay
import zavorateam.yabslushal.ui.icons.outlined.Radio
import zavorateam.yabslushal.ui.icons.outlined.Search
import zavorateam.yabslushal.ui.icons.outlined.Statistics

enum class NavigationTab(
	val destination: Screen,
	val iconFilled: ImageVector,
	val iconOutlined: ImageVector = iconFilled,
	val label: Int
) {
	LIBRARY(
		destination = Screen.Library(),
		iconFilled = Icons.Filled.LibraryMusic,
		iconOutlined = Icons.Outlined.LibraryMusic,
		label = R.string.title_library
	),
	ALBUMS(
		destination = Screen.AlbumList(),
		iconFilled = Icons.Filled.Album,
		iconOutlined = Icons.Outlined.Album,
		label = R.string.title_albums
	),
	PLAYLISTS(
		destination = Screen.PlaylistList(),
		iconFilled = Icons.Outlined.PlaylistPlay,
		label = R.string.title_playlists
	),
	ARTISTS(
		destination = Screen.ArtistList(),
		iconFilled = Icons.Filled.Artist,
		iconOutlined = Icons.Outlined.Artist,
		label = R.string.title_artists
	),
	SEARCH(
		destination = Screen.Search(),
		iconFilled = Icons.Outlined.Search,
		iconOutlined = Icons.Outlined.Search,
		label = R.string.title_search
	),
	GENRES(
		destination = Screen.GenreList(),
		iconFilled = Icons.Filled.Genre,
		iconOutlined = Icons.Outlined.Genre,
		label = R.string.title_genres
	),
	SONGS(
		destination = Screen.SongList(),
		iconFilled = Icons.Outlined.Note,
		iconOutlined = Icons.Outlined.Note,
		label = R.string.title_songs
	),
	RADIOS(
		destination = Screen.RadioList(),
		iconFilled = Icons.Filled.Radio,
		iconOutlined = Icons.Outlined.Radio,
		label = R.string.title_radios
	),
	STATISTICS(
		destination = Screen.Statistics(),
		iconFilled = Icons.Outlined.Statistics,
		label = R.string.title_statistics
	)
}
