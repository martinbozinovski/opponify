package com.opponify.service

import com.opponify.persistence.OpportunityRepository
import com.opponify.persistence.ParticipationRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.Duration

@Component
class DomainMaintenanceJob(
    private val opportunities:OpportunityRepository,
    private val requests:ParticipationRepository,
    private val jdbc:JdbcTemplate,
    @org.springframework.beans.factory.annotation.Value("\${opponify.post-game.resolution-window}") private val resolutionWindow:Duration
) {
    @Scheduled(fixedDelayString="\${OPPONIFY_MAINTENANCE_DELAY_MS:60000}")
    fun expireAndAdvance() {
        val now=Instant.now()
        opportunities.expire(now)
        requests.expire(now)
        jdbc.update("UPDATE time_proposals SET status='EXPIRED' WHERE status='PROPOSED' AND expires_at<=?",now)
        jdbc.update("UPDATE opportunity_time_proposals SET status='EXPIRED' WHERE status='PROPOSED' AND expires_at<=?",now)
        jdbc.update("UPDATE game_changes SET status='EXPIRED' WHERE status='PROPOSED' AND expires_at<=?",now)
        jdbc.update("UPDATE scheduled_games SET lifecycle='GAME_TIME',updated_at=NOW() WHERE lifecycle='SCHEDULED' AND start_at<=?",now)
        jdbc.update("""UPDATE attendance_events a SET state='UNRESOLVED' FROM scheduled_games g WHERE a.game_id=g.id AND a.state IN ('CLAIMED_ATTENDED','CLAIMED_ABSENT') AND g.start_at + (g.duration_seconds || ' seconds')::interval + (? * interval '1 second') <= NOW()""",resolutionWindow.seconds)
    }
}
