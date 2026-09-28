package com.opponify.participation.domain

import java.time.Instant
import java.util.UUID

data class ParticipationRequest(
    val id: UUID,
    val opportunityId: UUID,
    val requesterUserId: UUID?,
    val requesterTeamId: UUID?,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val status: RequestStatus = RequestStatus.PENDING
)
