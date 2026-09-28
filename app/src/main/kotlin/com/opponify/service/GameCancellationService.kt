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
class GameCancellationService(private val jdbc:JdbcTemplate,private val games:GameRepository){
    @Transactional fun cancel(actor:UUID,gameId:UUID,category:String,reason:String?){
        val game=games.find(gameId)?:throw ApiException(404,"GAME_NOT_FOUND","Game not found.")
        if(game.startAt<=Instant.now())throw ApiException(409,"GAME_STARTED","Started games cannot be cancelled through game cancellation.")
        val creator=jdbc.queryForObject("SELECT o.creator_user_id FROM scheduled_games g JOIN opportunities o ON o.id=g.opportunity_id WHERE g.id=?",UUID::class.java,gameId)
        val team=jdbc.queryForObject("SELECT o.creator_team_id FROM scheduled_games g JOIN opportunities o ON o.id=g.opportunity_id WHERE g.id=?",UUID::class.java,gameId)
        val allowed=actor==creator || (team!=null && (jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM team_memberships WHERE team_id=? AND user_id=? AND status='ACTIVE' AND role IN ('CAPTAIN','MANAGER'))",Boolean::class.java,team,actor)?:false))
        if(!allowed)throw ApiException(403,"GAME_AUTHORITY_REQUIRED","Game creator or authorized team representative required.")
        if(category !in setOf("PERSONAL","FACILITY_PROBLEM","PLATFORM_DISRUPTION","EMERGENCY","OTHER"))throw ApiException(422,"INVALID_CANCELLATION_CATEGORY","Unsupported cancellation category.")
        val late=Duration.between(Instant.now(),game.startAt).toHours()<24
        val excused=category=="FACILITY_PROBLEM" && (jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM facility_disruptions WHERE game_id=? AND status='OPEN')",Boolean::class.java,gameId)?:false)
        val id=UUID.randomUUID();jdbc.update("INSERT INTO game_cancellations(id,game_id,cancelled_by,category,reason,effective_late,excused) VALUES(?,?,?,?,?,?,?)",id,gameId,actor,category,reason,late,excused)
        jdbc.update("UPDATE scheduled_games SET lifecycle='CANCELLED',updated_at=NOW() WHERE id=? AND lifecycle IN ('SCHEDULED','GAME_TIME')",gameId)
        jdbc.update("DELETE FROM scheduled_commitment_intervals WHERE game_id=?",gameId)
    }
}
