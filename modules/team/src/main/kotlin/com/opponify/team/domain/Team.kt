package com.opponify.team.domain

import java.util.UUID

enum class TeamRole { CAPTAIN, MANAGER, MEMBER }
enum class MembershipStatus { PENDING, ACTIVE, LEFT, REMOVED }
data class Team(
    val id: UUID,
    val name: String,
    val active: Boolean = true
)
data class TeamMembership(
    val teamId: UUID,
    val userId: UUID,
    val role: TeamRole,
    val status: MembershipStatus
)
