package com.opponify.sport.domain

import java.util.UUID

enum class SportCode { PING_PONG, FUTSAL, STREET_BASKETBALL, TENNIS }
data class Sport(val id: UUID, val code: SportCode, val active: Boolean = true)
