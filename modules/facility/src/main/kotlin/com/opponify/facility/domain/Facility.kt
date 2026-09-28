package com.opponify.facility.domain

import java.util.UUID

enum class FacilityStatus { SUGGESTED, UNDER_REVIEW, APPROVED, REJECTED, ARCHIVED }
data class Facility(
    val id: UUID,
    val name: String,
    val town: String,
    val status: FacilityStatus
)
