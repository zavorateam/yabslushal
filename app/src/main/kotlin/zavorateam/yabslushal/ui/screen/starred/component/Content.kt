/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.ui.screen.starred.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.paging.compose.LazyPagingItems
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import zavorateam.yabslushal.R
import zavorateam.yabslushal.database.entity.DownloadEntity
import zavorateam.yabslushal.di.LocalNavStack
import zavorateam.yabslushal.domain.model.DomainAlbum
import zavorateam.yabslushal.domain.model.DomainAlbumListType
import zavorateam.yabslushal.domain.model.DomainArtist
import zavorateam.yabslushal.domain.model.DomainArtistListType
import zavorateam.yabslushal.domain.model.DomainSong
import zavorateam.yabslushal.domain.model.DomainSongListType
import zavorateam.yabslushal.domain.model.settings.ListViewMode
import zavorateam.yabslushal.ui.component.common.ContentUnavailable
import zavorateam.yabslushal.ui.component.common.SongRow
import zavorateam.yabslushal.ui.component.layout.ArtCarousel
import zavorateam.yabslushal.ui.component.layout.ArtCarouselItem
import zavorateam.yabslushal.ui.component.layout.ArtGridPlaceholder
import zavorateam.yabslushal.ui.component.sheet.ArtistSheet
import zavorateam.yabslushal.ui.component.sheet.CollectionSheet
import zavorateam.yabslushal.ui.icons.Icons
import zavorateam.yabslushal.ui.icons.filled.Play
import zavorateam.yabslushal.ui.icons.outlined.Shuffle
import zavorateam.yabslushal.ui.navigation.Screen
import zavorateam.yabslushal.ui.screen.playlist.dialog.PlaylistUpdateDialog

@Composable
fun StarredScreenContent(
	innerPadding: PaddingValues,
	isLoading: Boolean = false,
	onSetShareId: (String) -> Unit,
	isOnline: Boolean = false,

	songs: LazyPagingItems<DomainSong>?,
	selectedSong: DomainSong?,
	selectedSongIsStarred: Boolean,
	selectedSongRating: Int,
	allDownloads: List<DownloadEntity>,
	onSelectSong: (DomainSong) -> Unit,
	onClearSongSelection: () -> Unit,
	onAddSongStar: () -> Unit,
	onRemoveSongStar: () -> Unit,
	onPlaySongNext: (DomainSong) -> Unit,
	onAddSongToQueue: (DomainSong) -> Unit,
	onPlaySong: (Int) -> Unit,
	onPlayAllSongs: () -> Unit = {},
	onShuffleSongs: () -> Unit = {},
	/**
	 * Whether the "shuffle" and "play" buttons are shown above the song preview.
	 * The favorites tab hides them; the full song list reached via "see all" keeps them.
	 */
	showPlaybackActions: Boolean = false,
	onSetSongRating: (Int) -> Unit,
	onDownloadSong: (DomainSong) -> Unit,
	onCancelDownloadSong: (DomainSong) -> Unit,
	onDeleteDownloadSong: (DomainSong) -> Unit,

	// albums
	albums: LazyPagingItems<DomainAlbum>?,
	selectedAlbum: DomainAlbum?,
	selectedAlbumIsStarred: Boolean,
	selectedAlbumRating: Int,
	onSelectAlbum: (DomainAlbum) -> Unit,
	onClearAlbumSelection: () -> Unit,
	onStarSelectedAlbum: (Boolean) -> Unit,
	onRateSelectedAlbum: (Int) -> Unit,
	onPlayAlbumNext: () -> Unit,
	onAddAlbumToQueue: () -> Unit,

	// artists
	artists: LazyPagingItems<DomainArtist>?,
	selectedArtist: DomainArtist?,
	selectedArtistAlbums: List<DomainAlbum>?,
	selectedArtistIsStarred: Boolean,
	onSelectArtist: (DomainArtist) -> Unit,
	onClearArtistSelection: () -> Unit,
	onStarSelectedArtist: (Boolean) -> Unit,
	onPlayArtistNext: () -> Unit,
	onAddArtistToQueue: () -> Unit,
) {
	val gridState = rememberLazyGridState()
	val backStack = LocalNavStack.current

	val layoutDirection = LocalLayoutDirection.current
	val downloadsMap = remember(allDownloads) { allDownloads.associateBy { it.songId } }

	var songIdsToAddToPlaylist by rememberSaveable { mutableStateOf<ImmutableList<String>?>(null) }

	val scrollState = rememberScrollState()

	val songCount = songs?.itemCount ?: 0
	val albumCount = albums?.itemCount ?: 0
	val artistCount = artists?.itemCount ?: 0
	val totalCount = songCount + albumCount + artistCount

	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(state = scrollState),
		verticalArrangement = Arrangement.spacedBy(
			space = 12.dp,
			alignment = if (totalCount == 0 && !isLoading) Alignment.CenterVertically else Alignment.Top
		),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		if (totalCount == 0) {
			if (isLoading) {
				Column(
					modifier = Modifier.padding(innerPadding),
					verticalArrangement = Arrangement.spacedBy(12.dp)
				) {
					repeat(8) {
						ArtGridPlaceholder(viewMode = ListViewMode.List)
					}
				}
			} else {
				ContentUnavailable(
					label = stringResource(R.string.info_no_starred)
				)
			}
			return@Column
		}
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(
					start = innerPadding.calculateStartPadding(
						layoutDirection
					)
				)
				.padding(
					end = innerPadding.calculateEndPadding(
						layoutDirection
					)
				)
				.padding(
					bottom = innerPadding.calculateBottomPadding()
				), horizontalAlignment = Alignment.CenterHorizontally
		) {
			if (songCount > 0 && songs != null) {
				Row(
					modifier = Modifier
						.heightIn(min = 32.dp)
						.padding(top = 8.dp)
						.padding(horizontal = 16.dp)
						.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Text(
						stringResource(R.string.title_songs),
						style = MaterialTheme.typography.titleMediumEmphasized,
						fontWeight = FontWeight(600)
					)
					Text(
						stringResource(R.string.action_see_all),
						style = MaterialTheme.typography.labelLarge,
						color = MaterialTheme.colorScheme.primary,
						modifier = Modifier.clickable(onClick = dropUnlessResumed {
							backStack.add(
								Screen.SongList(
									nested = true, listType = DomainSongListType.FrequentlyPlayed
								)
							)
						})
					)
				}
				if (showPlaybackActions) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 16.dp, vertical = 6.dp),
						horizontalArrangement = Arrangement.spacedBy(10.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						OutlinedButton(
							modifier = Modifier.size(width = 52.dp, height = 44.dp),
							onClick = onShuffleSongs,
							shape = CircleShape,
							contentPadding = PaddingValues(0.dp)
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
								.height(44.dp),
							onClick = onPlayAllSongs,
							shape = CircleShape
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
								fontWeight = FontWeight.SemiBold
							)
						}
					}
				}
				val rowCount = remember(songCount) {
					songCount.coerceIn(1, 3)
				}
				val gridHeight = remember(rowCount) {
					when (rowCount) {
						1 -> 82.dp
						2 -> 164.dp
						else -> 246.dp
					}
				}
				LazyHorizontalGrid(
					rows = GridCells.Fixed(rowCount),
					state = gridState,
					flingBehavior = rememberSnapFlingBehavior(lazyGridState = gridState),
					modifier = Modifier
						.fillMaxWidth()
						.height(gridHeight)
				) {
					items(songCount) { index ->
						val song = songs[index] ?: return@items

						val download = downloadsMap[song.id]
						SongRow(
							modifier = Modifier.weight(1f),
							song = song,
							selected = selectedSong == song,
							onClick = { onPlaySong(index) },
							onLongClick = { onSelectSong(song) },
							onDismissRequest = { onClearSongSelection() },
							starredState = if (selectedSong == song) selectedSongIsStarred else song.starredAt != null,
							onAddStar = onAddSongStar,
							onRemoveStar = onRemoveSongStar,
							download = download,
							onDownload = { onDownloadSong(song) },
							onCancelDownload = { onCancelDownloadSong(song) },
							onDeleteDownload = { onDeleteDownloadSong(song) },
							onPlayNext = { onPlaySongNext(song) },
							onAddToQueue = { onAddSongToQueue(song) },
							onShare = { onSetShareId(song.id) },
							isOnline = isOnline,
							rating = selectedSongRating,
							onSetRating = { onSetSongRating(it) })
					}
				}
			}
			if (albumCount > 0 && albums != null) {
				ArtCarousel(
					stringResource(R.string.title_albums),
					albums.itemSnapshotList.items.toImmutableList(),
					Screen.AlbumList(true, DomainAlbumListType.AlphabeticalByArtist)
				) { album ->
					ArtCarouselItem(
						coverArtId = album.coverArtId,
						title = album.name ?: "[unknown album]",
						subtitle = album.artistName,
						contentDescription = null,
						onSelect = { onSelectAlbum(album) },
						onClick = dropUnlessResumed {
							backStack.add(Screen.CollectionDetail(album.id, "artist"))
						})
					if (selectedAlbum == album) {
						CollectionSheet(
							onDismissRequest = { onClearAlbumSelection() },
							collection = album,
							starred = selectedAlbumIsStarred,
							onShare = { onSetShareId(album.id) },
							onPlayNext = onPlayAlbumNext,
							onAddToQueue = onAddAlbumToQueue,
							onSetStarred = { onStarSelectedAlbum(!selectedAlbumIsStarred) },
							onAddAllToPlaylist = {
								songIdsToAddToPlaylist = selectedAlbum.songs.map { it.id }.toImmutableList()
							},
							rating = selectedAlbumRating,
							onSetRating = onRateSelectedAlbum
						)
					}
				}
			}
			if (artistCount == 0 || artists == null) return@Column
			ArtCarousel(
				stringResource(R.string.title_artists),
				artists.itemSnapshotList.items.toImmutableList(),
				Screen.ArtistList(true, DomainArtistListType.AlphabeticalByName)
			) { artist ->
				ArtCarouselItem(
					coverArtId = artist.coverArtId,
					title = artist.name,
					subtitle = pluralStringResource(
						R.plurals.count_albums, artist.albumCount, artist.albumCount
					),
					contentDescription = null,
					onSelect = { onSelectArtist(artist) },
					onClick = dropUnlessResumed {
						backStack.add(Screen.ArtistDetail(artist.id))
					})
				if (selectedArtist == artist) {
					ArtistSheet(
						onDismissRequest = onClearArtistSelection,
						artist = artist,
						onPlayNext = onPlayArtistNext,
						onAddToQueue = onAddArtistToQueue,
						onAddAllToPlaylist = {
							songIdsToAddToPlaylist =
								selectedArtistAlbums?.flatMap { album -> album.songs.map { it.id } }.orEmpty()
									.toImmutableList()
						},
						starred = selectedArtistIsStarred,
						onSetStarred = { onStarSelectedArtist(!selectedArtistIsStarred) })
				}
			}
		}
	}

	songIdsToAddToPlaylist?.let {
		PlaylistUpdateDialog(
			songIds = it, onDismissRequest = { songIdsToAddToPlaylist = null })
	}
}
