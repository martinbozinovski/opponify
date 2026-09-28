import com.opponify.opportunity.domain.*
import com.opponify.player.domain.SkillLevel
import com.opponify.scheduling.domain.ScheduledGame
import com.opponify.sport.domain.SportCode
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

fun main(){
    var failed=0
    try { Opportunity(UUID.randomUUID(),UUID.randomUUID(),null,SportCode.TENNIS,NeedType.OPPONENT,TimeType.EXACT,null,null,"Skopje",null,2,3,SkillLevel.EASY,SkillLevel.EASY,OpportunityStatus.DRAFT); failed++ } catch(_:IllegalArgumentException) {}
    val a=ScheduledGame(UUID.randomUUID(),Instant.parse("2026-01-01T10:00:00Z"),Duration.ofHours(1),ZoneOffset.UTC)
    val touching=ScheduledGame(UUID.randomUUID(),Instant.parse("2026-01-01T11:00:00Z"),Duration.ofHours(1),ZoneOffset.UTC)
    val overlap=ScheduledGame(UUID.randomUUID(),Instant.parse("2026-01-01T10:59:59Z"),Duration.ofHours(1),ZoneOffset.UTC)
    if(a.overlaps(touching))failed++
    if(!a.overlaps(overlap))failed++
    if(SportCode.entries.toSet()!=setOf(SportCode.PING_PONG,SportCode.FUTSAL,SportCode.STREET_BASKETBALL,SportCode.TENNIS))failed++
    if(SkillLevel.entries.toSet()!=setOf(SkillLevel.EASY,SkillLevel.MEDIUM,SkillLevel.HARD))failed++
    check(failed==0){"Domain invariant smoke failed: $failed"}
    println("PASS: domain invariants")
}
