package com.opponify.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NotificationDispatcher(private val jdbc:JdbcTemplate,private val mapper:ObjectMapper,private val notifications:NotificationService) {
    @Scheduled(fixedDelayString="\${OPPONIFY_NOTIFICATION_DELAY_MS:5000}")
    fun dispatch(){
        jdbc.queryForList("SELECT event_id,aggregate_id,event_type,payload FROM domain_events WHERE notification_dispatched_at IS NULL ORDER BY occurred_at,event_id LIMIT 50").forEach { row ->
            val eventId=row["event_id"] as UUID
            val type=row["event_type"] as String
            val actor=((mapper.readTree(row["payload"].toString()).get("actorUserId")?.asText())?.let(UUID::fromString))
            val recipients=when(type){
                "PARTICIPATION_REQUESTED" -> jdbc.queryForList("SELECT o.creator_user_id FROM participation_requests r JOIN opportunities o ON o.id=r.opportunity_id WHERE r.id=?",row["aggregate_id"] as UUID).mapNotNull{it["creator_user_id"] as UUID?}
                "PARTICIPATION_ACCEPTED" -> jdbc.queryForList("SELECT requester_user_id FROM participation_requests WHERE id=?",row["aggregate_id"] as UUID).mapNotNull{it["requester_user_id"] as UUID?}
                "RESULT_SUBMITTED","RESULT_CONFIRMED","GAME_MARKED_PLAYED" -> jdbc.queryForList("SELECT participant_user_id FROM game_participants WHERE game_id=? AND status='ACTIVE'",row["aggregate_id"] as UUID).mapNotNull{it["participant_user_id"] as UUID?}
                else -> emptyList()
            }.filter{it!=actor}.distinct()
            recipients.forEach{notifications.create(it,type,row["aggregate_id"] as UUID,null)}
            jdbc.update("UPDATE domain_events SET notification_dispatched_at=NOW() WHERE event_id=? AND notification_dispatched_at IS NULL",eventId)
        }
    }
}
