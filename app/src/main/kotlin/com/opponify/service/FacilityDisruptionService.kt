package com.opponify.service

import com.opponify.api.ApiException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FacilityDisruptionService(private val jdbc:JdbcTemplate,private val notifications:NotificationService){
    @Transactional fun report(actor:UUID,gameId:UUID,category:String,description:String?):UUID{
        val allowed=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM game_participants WHERE game_id=? AND participant_user_id=? AND status='ACTIVE') OR EXISTS(SELECT 1 FROM game_participants p JOIN team_memberships m ON m.team_id=p.participant_team_id WHERE p.game_id=? AND m.user_id=? AND m.status='ACTIVE' AND m.role IN ('CAPTAIN','MANAGER') AND p.status='ACTIVE')",Boolean::class.java,gameId,actor,gameId,actor)?:false
        if(!allowed)throw ApiException(403,"GAME_PARTICIPANT_REQUIRED","Only active participants may report a facility disruption.")
        val facility=jdbc.queryForObject("SELECT o.facility_id FROM scheduled_games g JOIN opportunities o ON o.id=g.opportunity_id WHERE g.id=?",UUID::class.java,gameId)
        val id=UUID.randomUUID();jdbc.update("INSERT INTO facility_disruptions(id,game_id,facility_id,reported_by,category,description) VALUES(?,?,?,?,?,?)",id,gameId,facility,actor,category,description)
        jdbc.queryForList("SELECT participant_user_id FROM game_participants WHERE game_id=? AND status='ACTIVE' AND participant_user_id IS NOT NULL AND participant_user_id<>?",gameId,actor).forEach{notifications.create(it["participant_user_id"] as UUID,"FACILITY_DISRUPTION",gameId,null)}
        return id
    }
}
