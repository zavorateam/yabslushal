/*
 * Yabslushal, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package zavorateam.yabslushal.database.mapper

import zavorateam.yabslushal.database.entity.RadioEntity
import zavorateam.yabslushal.domain.model.DomainRadio
import dev.zt64.subsonic.api.model.InternetRadioStation as ApiRadio

fun ApiRadio.toEntity() = RadioEntity(
    radioId = id,
    coverArtId = coverArtId,
    name = name,
    streamUrl = streamUrl,
    homepageUrl = homepageUrl
)

fun RadioEntity.toDomainModel() = DomainRadio(
	id = radioId,
	coverArtId = coverArtId,
	name = name,
	streamUrl = streamUrl,
	homepageUrl = homepageUrl
)
