package com.opponify.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class TrustRecalculationJob(private val jdbc:JdbcTemplate,private val trust:TrustService){
    @Scheduled(fixedDelayString="\${OPPONIFY_TRUST_DELAY_MS:5000}")
    fun process(){
        jdbc.queryForList("SELECT id,subject_id,subject_type FROM trust_recalculation_jobs WHERE status='PENDING' AND available_at<=NOW() ORDER BY created_at,id LIMIT 20").forEach{row->
            val id=row["id"] as UUID;val subject=row["subject_id"] as UUID;val type=row["subject_type"] as String
            if(jdbc.update("UPDATE trust_recalculation_jobs SET status='PROCESSING',attempts=attempts+1 WHERE id=? AND status='PENDING'",id)!=1)return@forEach
            try{trust.recalculate(subject,type);jdbc.update("UPDATE trust_recalculation_jobs SET status='COMPLETED',completed_at=? WHERE id=?",Instant.now(),id)}catch(_:Exception){jdbc.update("UPDATE trust_recalculation_jobs SET status=CASE WHEN attempts>=5 THEN 'FAILED' ELSE 'PENDING' END,available_at=NOW()+INTERVAL '30 seconds' WHERE id=?",id)}
        }
    }
}
