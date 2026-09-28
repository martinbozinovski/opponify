package com.opponify.service

import com.opponify.api.ApiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class TeamMembershipService(private val jdbc:JdbcTemplate,@Value("\${opponify.participation.request-expiry}") private val expiry:Duration){
    @Transactional fun request(user:UUID,team:UUID):UUID{if(!(jdbc.queryForObject("SELECT active FROM teams WHERE id=?",Boolean::class.java,team)?:false))throw ApiException(409,"TEAM_INACTIVE","Team is inactive.");val id=UUID.randomUUID();jdbc.update("INSERT INTO team_membership_requests(id,team_id,user_id,expires_at) VALUES(?,?,?,?) ON CONFLICT(team_id,user_id) DO UPDATE SET status='PENDING',expires_at=EXCLUDED.expires_at,updated_at=NOW()",id,team,user,Instant.now().plus(expiry));return id}
    @Transactional fun accept(actor:UUID,requestId:UUID){val r=jdbc.queryForList("SELECT * FROM team_membership_requests WHERE id=? AND status='PENDING'",requestId).firstOrNull()?:throw ApiException(404,"MEMBERSHIP_REQUEST_NOT_FOUND","Membership request not found.");if((r["expires_at"] as java.sql.Timestamp).toInstant()<=Instant.now()){jdbc.update("UPDATE team_membership_requests SET status='EXPIRED',updated_at=NOW() WHERE id=?",requestId);throw ApiException(409,"MEMBERSHIP_REQUEST_EXPIRED","Membership request expired.")};requireManager(actor,r["team_id"] as UUID);jdbc.update("INSERT INTO team_memberships(team_id,user_id,role,status) VALUES(?,?, 'MEMBER','ACTIVE') ON CONFLICT(team_id,user_id) DO UPDATE SET role='MEMBER',status='ACTIVE'",r["team_id"] as UUID,r["user_id"] as UUID);jdbc.update("UPDATE team_membership_requests SET status='ACCEPTED',updated_at=NOW() WHERE id=?",requestId)}
    @Transactional fun reject(actor:UUID,requestId:UUID){val r=jdbc.queryForList("SELECT * FROM team_membership_requests WHERE id=? AND status='PENDING'",requestId).firstOrNull()?:throw ApiException(404,"MEMBERSHIP_REQUEST_NOT_FOUND","Membership request not found.");requireManager(actor,r["team_id"] as UUID);jdbc.update("UPDATE team_membership_requests SET status='REJECTED',updated_at=NOW() WHERE id=?",requestId)}
    @Scheduled(fixedDelayString="\${OPPONIFY_TEAM_REQUEST_DELAY_MS:60000}") fun expire(){jdbc.update("UPDATE team_membership_requests SET status='EXPIRED',updated_at=NOW() WHERE status='PENDING' AND expires_at<=NOW()")}
    private fun requireManager(actor:UUID,team:UUID){if((jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM team_memberships WHERE team_id=? AND user_id=? AND status='ACTIVE' AND role IN ('CAPTAIN','MANAGER'))",Boolean::class.java,team,actor)?:false).not())throw ApiException(403,"TEAM_AUTHORITY_REQUIRED","Team management authority required.")}
}
