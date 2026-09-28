package com.opponify.history.domain

import java.time.Instant
import java.util.UUID

data class DomainEvent(
    val eventId: UUID,
    val aggregateId: UUID,
    val eventType: String,
    val occurredAt: Instant,
    val payloadVersion: Int = 1
)
