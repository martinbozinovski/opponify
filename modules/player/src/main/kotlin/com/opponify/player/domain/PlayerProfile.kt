package com.opponify.player.domain

import java.util.UUID

enum class SkillLevel { EASY, MEDIUM, HARD }
data class PlayerProfile(
    val userId: UUID,
    val skillLevel: SkillLevel? = null,
    val publicProfile: Boolean = true
)
