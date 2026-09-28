package com.opponify.opportunity.domain

import com.opponify.player.domain.SkillLevel
import com.opponify.sport.domain.SportCode
import java.time.Instant
import java.util.UUID

enum class NeedType { OPPONENT, PLAYERS, GAME }
enum class TimeType { EXACT, RANGE, FLEXIBLE }

data class Opportunity(
    val id: UUID,
    val creatorUserId: UUID?,
    val creatorTeamId: UUID?,
    val sport: SportCode,
    val need: NeedType,
    val timeType: TimeType,
    val startAt: Instant?,
    val endAt: Instant?,
    val town: String?,
    val facilityId: UUID?,
    val targetCapacity: Int,
    val minimumParticipation: Int,
    val skillLevel: SkillLevel?,
    val desiredOpponentLevel: SkillLevel?,
    val status: OpportunityStatus = OpportunityStatus.DRAFT
) {
    init {
        require(targetCapacity > 0) { "Target capacity must be positive." }
        require(minimumParticipation > 0) { "Minimum participation must be positive." }
        require(minimumParticipation <= targetCapacity) { "Minimum participation cannot exceed target capacity." }
    }
}
