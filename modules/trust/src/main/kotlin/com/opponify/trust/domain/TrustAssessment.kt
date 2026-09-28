package com.opponify.trust.domain

import java.time.Instant
import java.util.UUID

data class TrustAssessment(
    val subjectId: UUID,
    val score: Int?,
    val status: TrustStatus,
    val methodologyVersion: String,
    val assessedAt: Instant,
    val updating: Boolean = false
)
