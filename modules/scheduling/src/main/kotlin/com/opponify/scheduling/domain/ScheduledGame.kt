package com.opponify.scheduling.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class ScheduledGame(
    val id: UUID,
    val startAt: Instant,
    val duration: Duration,
    val timeZone: ZoneId
) {
    val endAt: Instant get() = startAt.plus(duration)

    fun overlaps(other: ScheduledGame): Boolean =
        startAt < other.endAt && other.startAt < endAt
}
