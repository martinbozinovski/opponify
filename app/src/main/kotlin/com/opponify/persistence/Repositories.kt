package com.opponify.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.opponify.opportunity.domain.*
import com.opponify.participation.domain.*
import com.opponify.player.domain.SkillLevel
import com.opponify.scheduling.domain.ScheduledGame
import com.opponify.sport.domain.SportCode
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.Base64

@Repository
class OpportunityRepository(private val jdbc: JdbcTemplate) {
    fun insert(o: Opportunity, expiresAt: Instant?) {
        jdbc.update("""INSERT INTO opportunities(id,creator_user_id,creator_team_id,sport,need_type,time_type,start_at,end_at,town,facility_id,target_capacity,minimum_participation,skill_level,desired_opponent_level,status,expires_at,timezone) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            o.id,o.creatorUserId,o.creatorTeamId,o.sport.name,o.need.name,o.timeType.name,o.startAt,o.endAt,o.town,o.facilityId,o.targetCapacity,o.minimumParticipation,o.skillLevel?.name,o.desiredOpponentLevel?.name,o.status.name,expiresAt,o.startAt?.atZone(ZoneId.of("UTC"))?.zone?.id ?: "UTC")
    }
    fun find(id: UUID): Opportunity? = jdbc.query("SELECT * FROM opportunities WHERE id=?", { rs, _ ->
        Opportunity(UUID.fromString(rs.getString("id")),rs.getObject("creator_user_id",UUID::class.java),rs.getObject("creator_team_id",UUID::class.java),SportCode.valueOf(rs.getString("sport")),NeedType.valueOf(rs.getString("need_type")),TimeType.valueOf(rs.getString("time_type")),rs.getTimestamp("start_at")?.toInstant(),rs.getTimestamp("end_at")?.toInstant(),rs.getString("town"),rs.getObject("facility_id",UUID::class.java),rs.getInt("target_capacity"),rs.getInt("minimum_participation"),rs.getString("skill_level")?.let(SkillLevel::valueOf),rs.getString("desired_opponent_level")?.let(SkillLevel::valueOf),OpportunityStatus.valueOf(rs.getString("status")))
    },id).firstOrNull()
    fun list(sport: SportCode?, town: String?, status: OpportunityStatus = OpportunityStatus.OPEN, limit: Int = 50, viewerId: UUID? = null, cursor: String? = null): OpportunityPage {
        val decoded=cursor?.let{decodeCursor(it)}
        val sql="SELECT * FROM opportunities WHERE status=?" + (viewerId?.let{" AND NOT EXISTS (SELECT 1 FROM blocks b WHERE b.blocker_user_id=? AND b.blocked_user_id=opportunities.creator_user_id)"} ?: "") + (sport?.let{" AND sport=?"} ?: "") + (town?.let{" AND town=?"} ?: "") + (decoded?.let{" AND (COALESCE(start_at,created_at),id) > (?,?)"} ?: "") + " ORDER BY COALESCE(start_at,created_at), id LIMIT ?"
        val args=mutableListOf<Any>(status.name); if(viewerId!=null)args+=viewerId; if(sport!=null)args+=sport.name; if(town!=null)args+=town; if(decoded!=null){args+=decoded.first;args+=decoded.second}; args+=limit.coerceIn(1,100)+1
        val rows=jdbc.query(sql,{rs,_ ->
            val item=Opportunity(UUID.fromString(rs.getString("id")),rs.getObject("creator_user_id",UUID::class.java),rs.getObject("creator_team_id",UUID::class.java),SportCode.valueOf(rs.getString("sport")),NeedType.valueOf(rs.getString("need_type")),TimeType.valueOf(rs.getString("time_type")),rs.getTimestamp("start_at")?.toInstant(),rs.getTimestamp("end_at")?.toInstant(),rs.getString("town"),rs.getObject("facility_id",UUID::class.java),rs.getInt("target_capacity"),rs.getInt("minimum_participation"),rs.getString("skill_level")?.let(SkillLevel::valueOf),rs.getString("desired_opponent_level")?.let(SkillLevel::valueOf),OpportunityStatus.valueOf(rs.getString("status")))
            item to (rs.getTimestamp("start_at")?.toInstant() ?: rs.getTimestamp("created_at").toInstant())
        },*args.toTypedArray())
        val more=rows.size>limit.coerceIn(1,100);val page=rows.take(limit.coerceIn(1,100));val next=if(more)encodeCursor(page.last().second,page.last().first.id) else null
        return OpportunityPage(page.map{it.first},next)
    }
    private fun encodeCursor(time:java.time.Instant,id:UUID)=Base64.getUrlEncoder().withoutPadding().encodeToString("$time|$id".toByteArray())
    private fun decodeCursor(value:String):Pair<java.time.Instant,UUID>{return try{val parts=String(Base64.getUrlDecoder().decode(value)).split('|');parts[0].let(java.time.Instant::parse) to UUID.fromString(parts[1])}catch(e:Exception){throw IllegalArgumentException("Invalid cursor")}}

    fun expire(now: Instant): Int = jdbc.update("UPDATE opportunities SET status='EXPIRED',updated_at=NOW() WHERE status='OPEN' AND expires_at IS NOT NULL AND expires_at<=?",now)
}

@Repository
class ParticipationRepository(private val jdbc: JdbcTemplate) {
    fun insert(id:UUID, opportunityId:UUID, userId:UUID?, teamId:UUID?, expiresAt:Instant?) = jdbc.update("INSERT INTO participation_requests(id,opportunity_id,requester_user_id,requester_team_id,expires_at,status) VALUES(?,?,?,?,?,'PENDING')",id,opportunityId,userId,teamId,expiresAt)
    fun find(id:UUID): Map<String,Any>? = jdbc.queryForList("SELECT * FROM participation_requests WHERE id=?",id).firstOrNull()
    fun pendingCount(opportunityId:UUID):Int = jdbc.queryForObject("SELECT COUNT(*) FROM participation_requests WHERE opportunity_id=? AND status='ACCEPTED'",Int::class.java,opportunityId) ?: 0
    fun accept(id:UUID): Int = jdbc.update("UPDATE participation_requests SET status='ACCEPTED',accepted_at=NOW(),updated_at=NOW() WHERE id=? AND status='PENDING'",id)
    fun reject(id:UUID): Int = jdbc.update("UPDATE participation_requests SET status='REJECTED',updated_at=NOW() WHERE id=? AND status='PENDING'",id)
    fun withdraw(id:UUID): Int = jdbc.update("UPDATE participation_requests SET status='WITHDRAWN',updated_at=NOW() WHERE id=? AND status IN ('PENDING','ACCEPTED')",id)
    fun expire(now:Instant):Int = jdbc.update("UPDATE participation_requests SET status='EXPIRED',updated_at=NOW() WHERE status='PENDING' AND expires_at<=?",now)
}

@Repository
class GameRepository(private val jdbc: JdbcTemplate, private val mapper:ObjectMapper) {
    fun create(id:UUID, opportunityId:UUID,start:Instant,duration:Duration,zone:ZoneId)=jdbc.update("INSERT INTO scheduled_games(id,opportunity_id,start_at,duration_seconds,timezone) VALUES(?,?,?,?,?)",id,opportunityId,start,duration.seconds,zone.id)
    fun find(id:UUID): ScheduledGame?=jdbc.query("SELECT * FROM scheduled_games WHERE id=?",{rs,_->ScheduledGame(UUID.fromString(rs.getString("id")),rs.getTimestamp("start_at").toInstant(),Duration.ofSeconds(rs.getLong("duration_seconds")),ZoneId.of(rs.getString("timezone")))},id).firstOrNull()
    fun hasOverlap(userId:UUID,start:Instant,end:Instant,excludeGame:UUID?=null):Boolean {
        val sql="""SELECT EXISTS(SELECT 1 FROM scheduled_games g JOIN game_participants p ON p.game_id=g.id WHERE p.participant_user_id=? AND p.status='ACTIVE' AND g.lifecycle IN ('SCHEDULED','GAME_TIME') AND g.start_at < ? AND g.start_at + (g.duration_seconds || ' seconds')::interval > ? ${if(excludeGame!=null)"AND g.id<>?" else ""})"""
        val args=mutableListOf<Any>(userId,end,start); if(excludeGame!=null)args+=excludeGame
        return jdbc.queryForObject(sql,Boolean::class.java,*args.toTypedArray()) ?: false
    }
    fun addParticipant(gameId:UUID,userId:UUID?,teamId:UUID?):Int {
        val game=jdbc.queryForList("SELECT start_at,duration_seconds FROM scheduled_games WHERE id=?",gameId).firstOrNull() ?: error("Game not found")
        val start=game["start_at"] as java.sql.Timestamp; val end=java.time.Instant.ofEpochMilli(start.time).plusSeconds((game["duration_seconds"] as Number).toLong())
        val key=if(userId!=null) "U:$userId" else "T:$teamId"
        val changed=jdbc.update("INSERT INTO game_participants(game_id,participant_user_id,participant_team_id) VALUES(?,?,?) ON CONFLICT DO NOTHING",gameId,userId,teamId)
        if(changed==1) jdbc.update("INSERT INTO scheduled_commitment_intervals(game_id,commitment_key,start_at,end_at) VALUES(?,?,?,?)",gameId,key,start.toInstant(),end)
        return changed
    }
    fun activeCount(gameId:UUID):Int=jdbc.queryForObject("SELECT COUNT(*) FROM game_participants WHERE game_id=? AND status='ACTIVE'",Int::class.java,gameId) ?: 0
    fun rebuildIntervals(gameId:UUID,start:Instant,end:Instant){
        jdbc.update("DELETE FROM scheduled_commitment_intervals WHERE game_id=?",gameId)
        jdbc.queryForList("SELECT participant_user_id,participant_team_id FROM game_participants WHERE game_id=? AND status='ACTIVE'",gameId).forEach{
            val key=if(it["participant_user_id"]!=null)"U:${it["participant_user_id"]}" else "T:${it["participant_team_id"]}"
            jdbc.update("INSERT INTO scheduled_commitment_intervals(game_id,commitment_key,start_at,end_at) VALUES(?,?,?,?)",gameId,key,start,end)
        }
    }
    fun updateLifecycle(gameId:UUID,lifecycle:String)=jdbc.update("UPDATE scheduled_games SET lifecycle=?,updated_at=NOW() WHERE id=?",lifecycle,gameId)
    fun participants(gameId:UUID): List<Map<String, Any>> =jdbc.queryForList("SELECT * FROM game_participants WHERE game_id=? AND status='ACTIVE'",gameId)
}


data class OpportunityPage(val items: List<Opportunity>, val nextCursor: String?)
