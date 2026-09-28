package com.opponify.identity.domain

import java.util.UUID

data class Principal(
    val userId: UUID,
    val authSubject: String
)
