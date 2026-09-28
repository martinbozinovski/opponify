package com.opponify.service

import com.opponify.api.ApiException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FacilityService(private val jdbc:JdbcTemplate) {
    fun list(town:String?,sport:String?,limit:Int):List<Map<String,Any>> {
        val params=mutableListOf<Any>(); var sql="SELECT DISTINCT f.* FROM facilities f"
        if(sport!=null){sql+=" JOIN facility_sport_associations a ON a.facility_id=f.id AND a.status='APPROVED' AND a.sport=?";params+=sport}
        sql+=" WHERE f.status='APPROVED'"
        if(town!=null){sql+=" AND f.town=?";params+=town}
        sql+=" ORDER BY f.name,f.id LIMIT ?";params+=limit.coerceIn(1,100)
        return jdbc.queryForList(sql,*params.toTypedArray())
    }
    @Transactional fun suggest(actor:UUID,name:String,town:String,lat:Double?,lon:Double?):UUID {
        val id=UUID.randomUUID()
        jdbc.update("INSERT INTO facilities(id,name,town,status,latitude,longitude) VALUES(?,?,?,'SUGGESTED',?,?)",id,name,town,lat,lon)
        jdbc.update("INSERT INTO facility_suggestions(id,suggested_by,facility_id,name,town,latitude,longitude) VALUES(?,?,?,?,?,?,?)",UUID.randomUUID(),actor,id,name,town,lat,lon)
        jdbc.update("INSERT INTO audit_log(actor_user_id,action,resource_type,resource_id) VALUES(?,?,?,?)",actor,"FACILITY_SUGGESTED","facility",id)
        return id
    }
    @Transactional fun review(actor:UUID,id:UUID,status:String){
        if(status !in setOf("UNDER_REVIEW","APPROVED","REJECTED","ARCHIVED"))throw ApiException(422,"INVALID_FACILITY_STATUS","Invalid facility status.")
        if(jdbc.update("UPDATE facilities SET status=?,updated_at=NOW() WHERE id=?",status,id)!=1)throw ApiException(404,"FACILITY_NOT_FOUND","Facility not found.")
        jdbc.update("INSERT INTO audit_log(actor_user_id,action,resource_type,resource_id) VALUES(?,?,?,?)",actor,"FACILITY_REVIEWED","facility",id)
    }
    @Transactional fun setSportAssociation(actor:UUID,id:UUID,sport:String,status:String){
        if(status !in setOf("APPROVED","UNDER_REVIEW","REJECTED"))throw ApiException(422,"INVALID_ASSOCIATION_STATUS","Invalid facility sport association status.")
        if(jdbc.queryForObject("SELECT COUNT(*) FROM facilities WHERE id=?",Int::class.java,id)==0)throw ApiException(404,"FACILITY_NOT_FOUND","Facility not found.")
        jdbc.update("INSERT INTO facility_sport_associations(facility_id,sport,status) VALUES(?,?,?) ON CONFLICT(facility_id,sport) DO UPDATE SET status=EXCLUDED.status",id,sport,status)
        jdbc.update("INSERT INTO audit_log(actor_user_id,action,resource_type,resource_id) VALUES(?,?,?,?)",actor,"FACILITY_SPORT_ASSOCIATION_CHANGED","facility",id)
    }
}
