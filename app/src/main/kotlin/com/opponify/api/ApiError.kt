package com.opponify.api

import java.time.Instant

data class ApiError(
    val code: String,
    val message: String,
    val correlationId: String,
    val timestamp: Instant = Instant.now(),
    val field: String? = null,
    val currentState: Any? = null
)
