package com.opponify.notification.domain

import java.time.Instant
import java.util.UUID

data class Notification(
    val id: UUID,
    val recipientUserId: UUID,
    val type: String,
    val resourceId: UUID?,
    val createdAt: Instant,
    val read: Boolean = false
)
