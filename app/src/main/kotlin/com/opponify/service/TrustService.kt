package com.opponify.service

import com.opponify.trust.domain.TrustStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToInt

@Service
class TrustService(private val jdbc:JdbcTemplate) {
    @Transactional
    fun recalculate(subjectId:UUID,subjectType:String="USER"): Map<String,Any?> {
        val version="v1"
        val evidence=jdbc.queryForList("SELECT evidence_type,severity,occurred_at,opponent_subject_id FROM trust_evidence WHERE subject_id=? AND subject_type=? AND eligible=true ORDER BY occurred_at DESC",subjectId,subjectType)
        var positive=0.0; var negative=0.0
        val opponentCounts=mutableMapOf<UUID,Int>()
        evidence.forEach { e ->
            val ageDays=Duration.between((e["occurred_at"] as java.sql.Timestamp).toInstant(),Instant.now()).toDays().coerceAtLeast(0)
            val recency=1.0/(1.0 + ageDays/365.0)
            val opponent=e["opponent_subject_id"] as UUID?
            val repetition=if(opponent!=null){val n=opponentCounts.getOrDefault(opponent,0);opponentCounts[opponent]=n+1;1.0/(1.0+n*0.5)}else 1.0
            val severity=(e["severity"] as Number).toInt()
            val weight=recency*repetition
            when(e["evidence_type"] as String){"CONFIRMED_GAME","CONFIRMED_RESULT"->positive += 10.0*weight;"CONFIRMED_NO_SHOW","LATE_CANCELLATION"->negative += severity.coerceAtLeast(1)*weight}
        }
        val total=evidence.size
        if(total==0){persist(subjectId,subjectType,null,TrustStatus.NEW.name,version,false);return get(subjectId,subjectType)}
        val score=((50.0 + positive*2.0 - negative*3.0).coerceIn(0.0,100.0)).roundToInt()
        val status=when {total<3->TrustStatus.PROVISIONAL.name;score>=90->TrustStatus.EXCELLENT.name;score>=75->TrustStatus.VERY_RELIABLE.name;score>=60->TrustStatus.RELIABLE.name;score>=40->TrustStatus.NEEDS_IMPROVEMENT.name;else->TrustStatus.PROVISIONAL.name}
        persist(subjectId,subjectType,score,status,version,false)
        return get(subjectId,subjectType)
    }

    @Transactional
    fun addEvidence(subjectId:UUID,eventId:UUID,type:String,severity:Int=0,opponentSubjectId:UUID?=null,subjectType:String="USER") {
        jdbc.update("INSERT INTO trust_evidence(subject_id,subject_type,source_event_id,evidence_type,severity,occurred_at,methodology_version,opponent_subject_id) VALUES(?, ?, ?, ?, ?, NOW(), 'v1', ?) ON CONFLICT DO NOTHING",subjectId,subjectType,eventId,type,severity,opponentSubjectId)
        persistUpdating(subjectId,subjectType)
        jdbc.update("INSERT INTO trust_recalculation_jobs(subject_id,subject_type,status,available_at) VALUES(?,?,'PENDING',NOW()) ON CONFLICT DO NOTHING",subjectId,subjectType)
    }

    fun get(subjectId:UUID,subjectType:String="USER"): Map<String,Any?> =jdbc.queryForList("SELECT subject_id,subject_type,score,status,methodology_version,assessed_at,updating FROM trust_assessments WHERE subject_id=? AND subject_type=?",subjectId,subjectType).firstOrNull() ?: mapOf("subjectId" to subjectId,"subjectType" to subjectType,"score" to null,"status" to TrustStatus.NEW.name,"methodology_version" to "v1","updating" to false)
    private fun persistUpdating(subjectId:UUID,subjectType:String){jdbc.update("INSERT INTO trust_assessments(subject_id,subject_type,score,status,methodology_version,assessed_at,updating) VALUES(?,?,NULL,?,?,NOW(),true) ON CONFLICT(subject_id,subject_type) DO UPDATE SET updating=true",subjectId,subjectType,TrustStatus.PROVISIONAL.name,"v1")}
    private fun persist(subjectId:UUID,subjectType:String,score:Int?,status:String,version:String,updating:Boolean){jdbc.update("INSERT INTO trust_assessments(subject_id,subject_type,score,status,methodology_version,assessed_at,updating) VALUES(?,?,?,?,?,NOW(),?) ON CONFLICT(subject_id,subject_type) DO UPDATE SET score=EXCLUDED.score,status=EXCLUDED.status,methodology_version=EXCLUDED.methodology_version,assessed_at=EXCLUDED.assessed_at,updating=EXCLUDED.updating",subjectId,subjectType,score,status,version,updating)}
}
