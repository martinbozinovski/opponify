package com.opponify.service

import com.opponify.api.ApiException
import com.opponify.persistence.GameRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class CancellationService(private val jdbc:JdbcTemplate,private val games:GameRepository,private val trust:TrustService) {
    @Transactional
    fun cancel(actor:UUID,gameId:UUID,category:String,reason:String?){
        if(category !in setOf("PERSONAL","FACILITY_PROBLEM","PLATFORM_DISRUPTION","EMERGENCY","OTHER")) throw ApiException(422,"INVALID_CANCELLATION_CATEGORY","Unsupported cancellation category.")
        val game=games.find(gameId)?:throw ApiException(404,"GAME_NOT_FOUND","Game not found.")
        val allowed=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM game_participants WHERE game_id=? AND participant_user_id=? AND status='ACTIVE') OR EXISTS(SELECT 1 FROM game_participants p JOIN team_memberships m ON m.team_id=p.participant_team_id WHERE p.game_id=? AND m.user_id=? AND m.status='ACTIVE' AND m.role IN ('CAPTAIN','MANAGER') AND p.status='ACTIVE')",Boolean::class.java,gameId,actor,gameId,actor)?:false
        if(!allowed)throw ApiException(403,"GAME_PARTICIPANT_REQUIRED","Only active participants or authorized team representatives may cancel.")
        val now=Instant.now();val late=Duration.between(now,game.startAt).toHours()<24
        if(game.startAt<=now)throw ApiException(409,"GAME_STARTED","A started game cannot be cancelled through this operation.")
        val excused=category=="FACILITY_PROBLEM" && (jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM facility_disruptions WHERE game_id=? AND status='OPEN')",Boolean::class.java,gameId)?:false)
        val id=UUID.randomUUID();jdbc.update("INSERT INTO game_cancellations(id,game_id,cancelled_by,category,reason,effective_late,excused) VALUES(?,?,?,?,?,?,?)",id,gameId,actor,category,reason,late,excused)
        jdbc.update("UPDATE game_participants SET status='CANCELLED',exited_at=NOW() WHERE game_id=? AND participant_user_id=? AND status='ACTIVE'",gameId,actor)
        jdbc.update("UPDATE game_participants p SET status='CANCELLED',exited_at=NOW() FROM team_memberships m WHERE p.game_id=? AND p.participant_team_id=m.team_id AND m.user_id=? AND m.status='ACTIVE' AND m.role IN ('CAPTAIN','MANAGER') AND p.status='ACTIVE'",gameId,actor)
        if(late&&!excused){
            trust.addEvidence(actor,id,"LATE_CANCELLATION",2)
            jdbc.queryForList("SELECT DISTINCT p.participant_team_id FROM game_participants p JOIN team_memberships m ON m.team_id=p.participant_team_id WHERE p.game_id=? AND m.user_id=?",gameId,actor).mapNotNull{it["participant_team_id"] as UUID?}.forEach{trust.addEvidence(it,id,"LATE_CANCELLATION",2,subjectType="TEAM")}
        }
        jdbc.update("DELETE FROM scheduled_commitment_intervals WHERE game_id=? AND commitment_key=?",gameId,"U:$actor")
        jdbc.queryForList("SELECT DISTINCT p.participant_team_id FROM game_participants p JOIN team_memberships m ON m.team_id=p.participant_team_id WHERE p.game_id=? AND m.user_id=?",gameId,actor).mapNotNull{it["participant_team_id"] as UUID?}.forEach{jdbc.update("DELETE FROM scheduled_commitment_intervals WHERE game_id=? AND commitment_key=?",gameId,"T:$it")}
    }
}
