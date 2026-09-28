package com.opponify.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@ConditionalOnProperty(name=["opponify.security.dev-mode"], havingValue="true")
class DevAuthenticationFilter: OncePerRequestFilter() {
    override fun doFilterInternal(request:HttpServletRequest,response:HttpServletResponse,chain:FilterChain) {
        val id=request.getHeader("X-User-Id")
        if(!id.isNullOrBlank()) SecurityContextHolder.getContext().authentication=UsernamePasswordAuthenticationToken(id,null,AuthorityUtils.createAuthorityList("ROLE_USER"))
        chain.doFilter(request,response)
    }
}
