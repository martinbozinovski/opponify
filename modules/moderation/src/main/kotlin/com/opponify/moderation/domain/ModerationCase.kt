package com.opponify.moderation.domain

import java.time.Instant
import java.util.UUID

enum class ModerationStatus { OPEN, IN_REVIEW, RESOLVED }
data class ModerationCase(
    val id: UUID,
    val reporterUserId: UUID,
    val subjectId: UUID?,
    val createdAt: Instant,
    val status: ModerationStatus = ModerationStatus.OPEN
)
