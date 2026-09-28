package com.opponify.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AccountService(private val jdbc:JdbcTemplate) {
    @Transactional
    fun close(actor:UUID){
        jdbc.update("UPDATE auth_identities SET active=false,deactivated_at=NOW() WHERE user_id=? AND active=true",actor)
        jdbc.update("UPDATE users SET auth_subject=?,deactivated_at=NOW() WHERE id=? AND deactivated_at IS NULL","anonymized:$actor",actor)
        jdbc.update("UPDATE player_profiles SET public_profile=false WHERE user_id=?",actor)
        jdbc.update("UPDATE team_memberships SET status='LEFT' WHERE user_id=? AND status IN ('ACTIVE','PENDING')",actor)
        jdbc.update("INSERT INTO audit_log(actor_user_id,action,resource_type,resource_id) VALUES(?,?,?,?)",actor,"ACCOUNT_ANONYMIZED","user",actor)
    }
}
