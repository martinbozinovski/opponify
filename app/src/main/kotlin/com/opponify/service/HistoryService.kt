package com.opponify.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class HistoryService(private val jdbc:JdbcTemplate) {
    fun events(subjectId:UUID,limit:Int)=jdbc.queryForList("SELECT event_id,aggregate_id,event_type,occurred_at,payload_version FROM domain_events WHERE aggregate_id=? ORDER BY occurred_at,event_id LIMIT ?",subjectId,limit.coerceIn(1,100))
    fun publicUserHistory(subjectId:UUID,limit:Int)=jdbc.queryForList("""SELECT g.id AS game_id,g.start_at,g.lifecycle,o.sport,o.town,o.facility_id FROM game_participants p JOIN scheduled_games g ON g.id=p.game_id JOIN opportunities o ON o.id=g.opportunity_id WHERE p.participant_user_id=? AND p.status IN ('ACTIVE','CANCELLED') AND g.lifecycle IN ('PLAYED','NOT_PLAYED','CANCELLED') ORDER BY g.start_at DESC,g.id DESC LIMIT ?""",subjectId,limit.coerceIn(1,100))
    fun publicTeamHistory(teamId:UUID,limit:Int)=jdbc.queryForList("""SELECT g.id AS game_id,g.start_at,g.lifecycle,o.sport,o.town,o.facility_id FROM game_participants p JOIN scheduled_games g ON g.id=p.game_id JOIN opportunities o ON o.id=g.opportunity_id WHERE p.participant_team_id=? AND p.status IN ('ACTIVE','CANCELLED') AND g.lifecycle IN ('PLAYED','NOT_PLAYED','CANCELLED') ORDER BY g.start_at DESC,g.id DESC LIMIT ?""",teamId,limit.coerceIn(1,100))
}

