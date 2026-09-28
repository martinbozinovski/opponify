package com.opponify.history.domain

import java.time.Instant
import java.util.UUID

data class HistoryRecord(
    val id: UUID,
    val subjectId: UUID,
    val eventType: String,
    val occurredAt: Instant,
    val sourceEventId: UUID
)
