/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.di

import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module
import zavorateam.yabslushal.work.DownloadMigrationWorker
import zavorateam.yabslushal.work.DownloadWorker
import zavorateam.yabslushal.work.LibrarySyncWorker
import zavorateam.yabslushal.work.SyncActionWorker

val workerModule = module {
	workerOf(::DownloadWorker)
	workerOf(::DownloadMigrationWorker)
	workerOf(::LibrarySyncWorker)
	workerOf(::SyncActionWorker)
}
