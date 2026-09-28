package com.opponify.service

import com.opponify.api.ApiException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class NotificationService(private val jdbc:JdbcTemplate) {
    @Transactional fun create(recipient:UUID,type:String,resourceId:UUID?,payload:String?):UUID {val id=UUID.randomUUID();jdbc.update("INSERT INTO notifications(id,recipient_user_id,type,resource_id,payload) VALUES(?,?,?,?,?::jsonb)",id,recipient,type,resourceId,payload?:"{}");return id}
    fun list(recipient:UUID,limit:Int)=jdbc.queryForList("SELECT id,type,resource_id,created_at,read_at,payload FROM notifications WHERE recipient_user_id=? ORDER BY created_at DESC,id DESC LIMIT ?",recipient,limit.coerceIn(1,100))
    @Transactional fun markRead(actor:UUID,id:UUID){if(jdbc.update("UPDATE notifications SET read_at=NOW() WHERE id=? AND recipient_user_id=?",id,actor)!=1)throw ApiException(404,"NOTIFICATION_NOT_FOUND","Notification not found.")}
}
