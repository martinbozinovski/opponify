package com.opponify.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.jdbc.core.JdbcTemplate
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.util.UUID

@Component
class EventPublisher(
    private val jdbc:JdbcTemplate,
    private val mapper:ObjectMapper,
    @Value("\${OPPONIFY_EVENTS_QUEUE_URL:}") private val queueUrl:String
) {
    private val sqs:SqsAsyncClient by lazy { SqsAsyncClient.builder().build() }
    @Scheduled(fixedDelayString="\${OPPONIFY_EVENT_PUBLISH_DELAY_MS:5000}")
    fun publish(){
        if(queueUrl.isBlank()) return
        jdbc.queryForList("SELECT event_id,aggregate_id,event_type,occurred_at,payload_version,payload FROM domain_events WHERE published_at IS NULL ORDER BY occurred_at,event_id LIMIT 50").forEach { row ->
            val eventId=row["event_id"] as UUID
            val body=mapper.writeValueAsString(mapOf("eventId" to eventId,"aggregateId" to row["aggregate_id"],"eventType" to row["event_type"],"occurredAt" to row["occurred_at"],"payloadVersion" to row["payload_version"],"payload" to row["payload"]))
            try {
                val builder=SendMessageRequest.builder().queueUrl(queueUrl).messageBody(body)
                if(queueUrl.endsWith(".fifo")) builder.messageGroupId("opponify").messageDeduplicationId(eventId.toString())
                sqs.sendMessage(builder.build()).join()
                jdbc.update("UPDATE domain_events SET published_at=NOW() WHERE event_id=? AND published_at IS NULL",eventId)
            } catch(_:Exception) { /* leave unpublished for retry; queue/DLQ policy remains infrastructure-controlled */ }
        }
    }
}
