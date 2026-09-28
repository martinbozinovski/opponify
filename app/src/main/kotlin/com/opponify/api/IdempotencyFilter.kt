package com.opponify.api

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.web.util.ContentCachingResponseWrapper
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant

@Component
class IdempotencyFilter(private val jdbc:JdbcTemplate,private val mapper:ObjectMapper):OncePerRequestFilter(){
    override fun shouldNotFilter(request:HttpServletRequest)=request.method in setOf("GET","HEAD","OPTIONS") || request.requestURI.startsWith("/api/v1/health") || request.requestURI.startsWith("/actuator") || request.requestURI.startsWith("/api/v1/docs") || request.requestURI.startsWith("/api/v1/openapi")
    override fun doFilterInternal(request:HttpServletRequest,response:HttpServletResponse,chain:FilterChain){
        val key=request.getHeader("Idempotency-Key")?.trim()
        if(key.isNullOrBlank()){response.status=400;response.contentType="application/json";response.writer.write(mapper.writeValueAsString(ApiError("IDEMPOTENCY_KEY_REQUIRED","Idempotency-Key is required for mutations","",Instant.now())));return}
        val operation=request.method+" "+request.requestURI
        val existing=jdbc.queryForList("SELECT response_status,response_body,expires_at FROM idempotency_records_v2 WHERE key=? AND operation=?",key,operation).firstOrNull()
        if(existing!=null){
            val expires=existing["expires_at"] as java.sql.Timestamp
            if(expires.toInstant()>Instant.now()){
                val status=(existing["response_status"] as Number?)?.toInt()
                if(status==null){response.status=409;response.contentType="application/json";response.writer.write(mapper.writeValueAsString(ApiError("IDEMPOTENCY_IN_PROGRESS","A request with this key is already processing","",Instant.now())))}
                else{response.status=status;response.contentType="application/json";val body=existing["response_body"]?.toString();if(!body.isNullOrEmpty() && body!="{}")response.writer.write(body)}
                return
            }
            jdbc.update("DELETE FROM idempotency_records_v2 WHERE key=? AND operation=?",key,operation)
        }
        val inserted=jdbc.update("INSERT INTO idempotency_records_v2(key,operation,response_status,response_body,expires_at) VALUES(?,?,NULL,NULL,NOW()+INTERVAL '48 hours') ON CONFLICT DO NOTHING",key,operation)
        if(inserted!=1){response.status=409;return}
        val wrapper=ContentCachingResponseWrapper(response)
        try{chain.doFilter(request,wrapper)}finally{
            val status=wrapper.status
            if(status<500){val body=wrapper.contentAsByteArray.toString(Charsets.UTF_8);jdbc.update("UPDATE idempotency_records_v2 SET response_status=?,response_body=?::jsonb WHERE key=? AND operation=?",status,if(body.isBlank())"{}" else body,key,operation)} else jdbc.update("DELETE FROM idempotency_records_v2 WHERE key=? AND operation=?",key,operation)
            wrapper.copyBodyToResponse()
        }
    }
}
