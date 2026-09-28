package com.opponify.service

import com.opponify.api.ApiException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TeamService(private val jdbc:JdbcTemplate) {
    @Transactional fun create(actor:UUID,name:String):UUID {val id=UUID.randomUUID();jdbc.update("INSERT INTO teams(id,name) VALUES(?,?)",id,name);jdbc.update("INSERT INTO team_memberships(team_id,user_id,role,status) VALUES(?,?, 'CAPTAIN','ACTIVE')",id,actor);return id}
    @Transactional fun addMember(actor:UUID,teamId:UUID,userId:UUID,role:String){requireCaptain(actor,teamId);if(role !in setOf("MANAGER","MEMBER"))throw ApiException(422,"INVALID_TEAM_ROLE","Invalid team role.");jdbc.update("INSERT INTO team_memberships(team_id,user_id,role,status) VALUES(?,?,?,'ACTIVE') ON CONFLICT(team_id,user_id) DO UPDATE SET role=EXCLUDED.role,status='ACTIVE'",teamId,userId,role)}
    @Transactional fun changeCaptain(actor:UUID,teamId:UUID,userId:UUID){requireCaptain(actor,teamId);if((jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM team_memberships WHERE team_id=? AND user_id=? AND status='ACTIVE')",Boolean::class.java,teamId,userId)?:false).not())throw ApiException(409,"CAPTAIN_TARGET_NOT_MEMBER","New captain must be an active team member.");jdbc.update("UPDATE team_memberships SET role='MANAGER' WHERE team_id=? AND role='CAPTAIN'",teamId);jdbc.update("UPDATE team_memberships SET role='CAPTAIN',status='ACTIVE' WHERE team_id=? AND user_id=?",teamId,userId)}
    @Transactional fun close(actor:UUID,teamId:UUID){requireCaptain(actor,teamId);jdbc.update("UPDATE teams SET active=false WHERE id=?",teamId)}
    private fun requireCaptain(actor:UUID,teamId:UUID){if((jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM team_memberships WHERE team_id=? AND user_id=? AND status='ACTIVE' AND role='CAPTAIN')",Boolean::class.java,teamId,actor)?:false).not())throw ApiException(403,"CAPTAIN_REQUIRED","Captain authority required.")}
}
