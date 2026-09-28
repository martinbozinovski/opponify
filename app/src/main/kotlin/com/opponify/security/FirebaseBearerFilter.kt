package com.opponify.security

import com.google.firebase.auth.FirebaseAuth
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
@ConditionalOnProperty(name=["opponify.security.dev-mode"],havingValue="false",matchIfMissing=true)
class FirebaseBearerFilter(private val firebaseAuth:FirebaseAuth):OncePerRequestFilter(){
    override fun shouldNotFilter(request:HttpServletRequest)=request.method in setOf("OPTIONS") || request.requestURI.startsWith("/api/v1/health") || request.requestURI.startsWith("/actuator") || request.requestURI.startsWith("/api/v1/docs") || request.requestURI.startsWith("/api/v1/openapi")
    override fun doFilterInternal(request:HttpServletRequest,response:HttpServletResponse,chain:FilterChain){
        val header=request.getHeader("Authorization")
        if(header?.startsWith("Bearer ")==true){
            try{
                val token=firebaseAuth.verifyIdToken(header.removePrefix("Bearer ").trim())
                SecurityContextHolder.getContext().authentication=UsernamePasswordAuthenticationToken(token.uid,null,AuthorityUtils.createAuthorityList("ROLE_USER"))
            }catch(_:Exception){response.status=401;return}
        }
        chain.doFilter(request,response)
    }
}
