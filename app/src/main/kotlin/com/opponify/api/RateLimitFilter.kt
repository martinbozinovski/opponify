package com.opponify.api

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class RateLimitFilter:OncePerRequestFilter(){
    private data class Window(val startedAt:Long,val count:AtomicInteger)
    private val windows=ConcurrentHashMap<String,Window>()
    override fun shouldNotFilter(request:HttpServletRequest)=request.method in setOf("GET","HEAD","OPTIONS") || request.requestURI.startsWith("/api/v1/health") || request.requestURI.startsWith("/actuator")
    override fun doFilterInternal(request:HttpServletRequest,response:HttpServletResponse,chain:FilterChain){
        val principal=SecurityContextHolder.getContext().authentication?.name
        val key=(principal ?: request.remoteAddr ?: "unknown")+":"+request.requestURI
        val now=System.currentTimeMillis();val w=windows.compute(key){_,old->if(old==null||now-old.startedAt>=60_000)Window(now,AtomicInteger(0))else old}!!
        val limit=if(request.requestURI.contains("/reports")||request.requestURI.contains("/messages"))30 else 120
        if(w.count.incrementAndGet()>limit){response.status=429;response.setHeader("Retry-After","60");return}
        chain.doFilter(request,response)
    }
    @Scheduled(fixedDelay=300000)
    fun cleanup(){val cutoff=System.currentTimeMillis()-120_000;windows.entries.removeIf{it.value.startedAt<cutoff}}
}
