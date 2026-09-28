package com.opponify.persistence

import com.opponify.api.ApiException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CurrentUserService(private val jdbc: JdbcTemplate) {
    @Transactional
    fun resolve(authSubject: String): UUID {
        val existing = jdbc.queryForList("SELECT user_id,active FROM auth_identities WHERE auth_subject=?", authSubject).firstOrNull()
        if(existing!=null){
            if(existing["active"] != true) throw ApiException(403,"ACCOUNT_CLOSED","This authentication identity is closed.")
            return existing["user_id"] as UUID
        }
        val id = UUID.randomUUID()
        jdbc.update("INSERT INTO users(id,auth_subject) VALUES(?,?)", id, "internal:$id")
        jdbc.update("INSERT INTO player_profiles(user_id) VALUES(?)", id)
        jdbc.update("INSERT INTO auth_identities(auth_subject,user_id) VALUES(?,?)",authSubject,id)
        return id
    }
}
