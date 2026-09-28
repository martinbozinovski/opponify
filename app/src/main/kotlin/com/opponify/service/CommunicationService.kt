package com.opponify.service

import com.opponify.api.ApiException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CommunicationService(private val jdbc:JdbcTemplate) {
    @Transactional fun createContext(actor:UUID,other:UUID,gameId:UUID?):UUID {
        ensureAllowed(actor,other,gameId)
        val id=UUID.randomUUID();jdbc.update("INSERT INTO message_contexts(id,game_id,user_a,user_b) VALUES(?,?,?,?)",id,gameId,actor,other);return id
    }
    @Transactional fun send(actor:UUID,contextId:UUID,body:String):UUID {
        val row=jdbc.queryForList("SELECT * FROM message_contexts WHERE id=? AND active=true",contextId).firstOrNull() ?: throw ApiException(404,"MESSAGE_CONTEXT_NOT_FOUND","Message context not found.")
        if(row["user_a"]!=actor && row["user_b"]!=actor) throw ApiException(403,"MESSAGE_ACCESS_DENIED","User is not part of this communication context.")
        if(body.isBlank() || body.length>4000) throw ApiException(422,"INVALID_MESSAGE","Message must contain 1-4000 characters.")
        val id=UUID.randomUUID();jdbc.update("INSERT INTO messages(id,context_id,sender_user_id,body,retained_until) VALUES(?,?,?,?,NOW()+INTERVAL '180 days')",id,contextId,actor,body);return id
    }
    fun list(actor:UUID,contextId:UUID)=jdbc.queryForList("SELECT m.id,m.sender_user_id,m.body,m.created_at FROM messages m JOIN message_contexts c ON c.id=m.context_id WHERE c.id=? AND c.active=true AND (c.user_a=? OR c.user_b=?) ORDER BY m.created_at,m.id",contextId,actor,actor)
    @Transactional fun block(actor:UUID,target:UUID){if(actor==target)throw ApiException(422,"SELF_BLOCK","Cannot block yourself.");jdbc.update("INSERT INTO blocks(blocker_user_id,blocked_user_id) VALUES(?,?) ON CONFLICT DO NOTHING",actor,target)}
    fun blockedEither(a:UUID,b:UUID)=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM blocks WHERE (blocker_user_id=? AND blocked_user_id=?) OR (blocker_user_id=? AND blocked_user_id=?))",Boolean::class.java,a,b,b,a)?:false
    private fun ensureAllowed(actor:UUID,other:UUID,gameId:UUID?){if(actor==other)throw ApiException(422,"SELF_MESSAGE","Cannot message yourself.");if(blockedEither(actor,other))throw ApiException(403,"USER_BLOCKED","Communication is blocked.");if(gameId!=null){val allowed=jdbc.queryForObject("SELECT (SELECT COUNT(DISTINCT participant_user_id) FROM game_participants WHERE game_id=? AND participant_user_id IN (?,?) AND status='ACTIVE') = 2",Boolean::class.java,gameId,actor,other)?:false;if(!allowed)throw ApiException(403,"COMMUNICATION_NOT_ELIGIBLE","Users do not have an eligible game relationship.")}}
}
