package com.opponify.service

import com.opponify.api.ApiException
import com.opponify.player.domain.SkillLevel
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PlayerProfileService(private val jdbc:JdbcTemplate){
    fun get(user:UUID,viewer:UUID?=null):Map<String,Any>{
        val row=jdbc.queryForList("SELECT user_id,skill_level,public_profile FROM player_profiles WHERE user_id=?",user).firstOrNull()?:throw ApiException(404,"PROFILE_NOT_FOUND","Player profile not found.")
        if(row["public_profile"]!=true && viewer!=user)throw ApiException(403,"PROFILE_PRIVATE","Profile is private.")
        return row
    }
    @Transactional fun update(actor:UUID,skill:SkillLevel?,publicProfile:Boolean){jdbc.update("UPDATE player_profiles SET skill_level=?,public_profile=? WHERE user_id=?",skill?.name,publicProfile,actor)}
}
