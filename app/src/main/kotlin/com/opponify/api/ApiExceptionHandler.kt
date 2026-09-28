package com.opponify.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.util.UUID

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ApiException::class)
    fun handle(ex:ApiException):ResponseEntity<ApiError> = ResponseEntity.status(ex.status).body(ApiError(ex.code,ex.message,UUID.randomUUID().toString(),Instant.now()))
    @ExceptionHandler(IllegalArgumentException::class)
    fun bad(ex:IllegalArgumentException):ResponseEntity<ApiError> = ResponseEntity.badRequest().body(ApiError("INVALID_REQUEST",ex.message ?: "Invalid request",UUID.randomUUID().toString(),Instant.now()))
}
