package com.opponify.service

import com.fasterxml.jackson.databind.JsonNode
import com.opponify.api.ApiException
import com.opponify.sport.domain.SportCode
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ResultValidationService(private val jdbc:JdbcTemplate) {
    fun validate(gameId:UUID,payload:JsonNode):SportCode {
        val sportName=jdbc.queryForObject("SELECT sport FROM opportunities o JOIN scheduled_games g ON g.opportunity_id=o.id WHERE g.id=?",String::class.java,gameId) ?: throw ApiException(404,"GAME_NOT_FOUND","Game not found.")
        val sport=SportCode.valueOf(sportName)
        when(sport){
            SportCode.PING_PONG, SportCode.TENNIS -> requireArray(payload,"sets") { item -> item.isObject && item.has("home") && item.has("away") && item.get("home").canConvertToInt() && item.get("away").canConvertToInt() }
            SportCode.FUTSAL, SportCode.STREET_BASKETBALL -> requireArray(payload,"scores") { item -> item.isObject && item.has("participantId") && item.has("points") && item.get("points").canConvertToInt() && item.get("points").asInt()>=0 }
        }
        return sport
    }
    private fun requireArray(payload:JsonNode,name:String,predicate:(JsonNode)->Boolean){val node=payload.get(name);if(node==null||!node.isArray||node.size()==0||node.any{!predicate(it)})throw ApiException(422,"INVALID_RESULT_SCHEMA","Invalid sport-specific result schema.")}
}
