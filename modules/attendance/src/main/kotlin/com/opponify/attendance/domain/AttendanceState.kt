package com.opponify.attendance.domain

enum class AttendanceState {
    EXPECTED, CLAIMED_ATTENDED, CLAIMED_ABSENT,
    CONFIRMED_ATTENDED, CONFIRMED_ABSENT, DISPUTED, UNRESOLVED
}
