package com.opponify.service

import com.opponify.api.ApiException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DisputeService(private val jdbc:JdbcTemplate,private val trust:TrustService){
    @Transactional fun open(actor:UUID,gameId:UUID,subjectType:String,subjectId:UUID,reason:String):UUID{
        ensureParticipant(actor,gameId)
        if(subjectType !in setOf("RESULT","ATTENDANCE"))throw ApiException(422,"INVALID_DISPUTE_TYPE","Unsupported dispute type.")
        val id=UUID.randomUUID();jdbc.update("INSERT INTO disputes(id,game_id,reporter_user_id,subject_type,subject_id,reason) VALUES(?,?,?,?,?,?)",id,gameId,actor,subjectType,subjectId,reason)
        if(subjectType=="RESULT") jdbc.update("UPDATE results SET state='DISPUTED',updated_at=NOW() WHERE game_id=?",gameId)
        else jdbc.update("INSERT INTO attendance_events(id,game_id,participant_key,submitted_by,state) VALUES(?,?,?,?, 'DISPUTED')",UUID.randomUUID(),gameId,subjectId.toString(),actor)
        return id
    }
    @Transactional fun resolve(actor:UUID,disputeId:UUID,resolution:String){
        val row=jdbc.queryForList("SELECT * FROM disputes WHERE id=? AND status='OPEN'",disputeId).firstOrNull()?:throw ApiException(404,"DISPUTE_NOT_FOUND","Open dispute not found.")
        val gameId=row["game_id"] as UUID;ensureParticipant(actor,gameId)
        if(actor == row["reporter_user_id"] && resolution == "CONFIRM") throw ApiException(409,"SELF_CONFIRMATION_NOT_ALLOWED","A result dispute cannot be resolved by its reporting submitter.")
        val type=row["subject_type"] as String
        when(type){
            "RESULT" -> when(resolution){
                "CONFIRM"->jdbc.update("UPDATE results SET state='CONFIRMED',confirmed_by=?,updated_at=NOW() WHERE game_id=?",actor,gameId)
                "KEEP_DISPUTED"->jdbc.update("UPDATE results SET state='DISPUTED',updated_at=NOW() WHERE game_id=?",gameId)
                else->throw ApiException(422,"INVALID_DISPUTE_RESOLUTION","Unsupported result resolution.")
            }
            "ATTENDANCE" -> when(resolution){
                "CONFIRM_ATTENDED"->jdbc.update("INSERT INTO attendance_events(id,game_id,participant_key,submitted_by,state) VALUES(?,?,?,?, 'CONFIRMED_ATTENDED')",UUID.randomUUID(),gameId, row["subject_id"].toString(),actor)
                "CONFIRM_ABSENT"->{val event=UUID.randomUUID();jdbc.update("INSERT INTO attendance_events(id,game_id,participant_key,submitted_by,state) VALUES(?,?,?,?, 'CONFIRMED_ABSENT')",event,gameId,row["subject_id"].toString(),actor);trust.addEvidence(row["subject_id"] as UUID,event,"CONFIRMED_NO_SHOW",5)}
                "UNRESOLVED"->jdbc.update("INSERT INTO attendance_events(id,game_id,participant_key,submitted_by,state) VALUES(?,?,?,?, 'UNRESOLVED')",UUID.randomUUID(),gameId,row["subject_id"].toString(),actor)
                else->throw ApiException(422,"INVALID_DISPUTE_RESOLUTION","Unsupported attendance resolution.")
            }
        }
        jdbc.update("UPDATE disputes SET status='RESOLVED',resolution=?,resolved_by=?,resolved_at=NOW() WHERE id=?",resolution,actor,disputeId)
    }
    private fun ensureParticipant(actor:UUID,gameId:UUID){if((jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM game_participants WHERE game_id=? AND participant_user_id=? AND status='ACTIVE')",Boolean::class.java,gameId,actor)?:false).not())throw ApiException(403,"GAME_PARTICIPANT_REQUIRED","Only game participants may open or resolve participant disputes.")}
}
