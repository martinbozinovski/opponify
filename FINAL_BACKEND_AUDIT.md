# Opponify Phase 9L — Final Backend Audit

## Result

**Implementation status: materially complete.**

**Phase 9 status: NOT YET MARKED COMPLETE.**

The source implementation and static/domain validation are complete, but this execution environment cannot perform the final Spring/Gradle/PostgreSQL runtime validation because Gradle is not installed and external package resolution is unavailable. The repository therefore includes CI validation so the full build/test gate can run in an environment with dependency access.

## Validation performed in this workspace

- 37/37 Phase 9L cross-cutting/static invariant checks: **PASS**.
- Pure Kotlin domain invariant smoke test: **PASS**.
- All launch sports remain exactly Ping Pong, Futsal, Street Basketball, Tennis.
- Skill levels remain exactly Easy, Medium, Hard.
- Opportunity/commitment distinction remains explicit.
- Exact/range/flexible scheduling remains explicit.
- Range/flexible opportunities require mutual exact-time confirmation before game creation.
- Actual overlap is checked in application logic and protected by a PostgreSQL exclusion constraint.
- Material game/opportunity changes require controlled confirmations.
- Post-game resolution does not manufacture Played/Not Played/no-show outcomes.
- Attendance and results remain separate.
- No-show trust evidence requires confirmed absence; one-sided claims remain non-punitive.
- Sport-specific result schemas exist for all four launch sports.
- Trust is derived from evidence, bounded, recency-aware, repeated-opponent diminishing, methodology-versioned, and asynchronously recalculated.
- Team authority is server-side; captain and manager permissions are separated.
- Pending team membership requests expire; active membership does not automatically expire.
- Facilities remain informational; no reservation/payment system was introduced.
- Facility suitability is based on approved sport associations.
- Facility disruption creates a workflow signal and does not automatically cancel a game.
- Blocks affect discovery/communication without rewriting history or cancelling commitments automatically.
- Messaging is kept separate from authoritative game state.
- Mutation idempotency is enforced with database-backed replay records.
- Rate limiting is risk-sensitive.
- Firebase identity is kept separate from the internal User UUID.
- Account closure anonymizes internal identity while preserving historical/domain records.
- Durable domain events and asynchronous notification dispatch exist.
- An SQS adapter is present for production event delivery when configured.
- Flyway is the only schema-evolution mechanism.

## Important implementation correction during 9L

The foundation schema originally defined `game_participants` with nullable user/team participant columns inside a composite primary key. PostgreSQL primary-key semantics made that incompatible with the intended XOR participant model. This was corrected through forward migration `V17__game_participant_key_fix.sql`, preserving the product/domain baseline and the migration-only schema rule.

## Locked baseline audit

### Phases 1–4
No product/business rule was intentionally changed. The implementation preserves the locked opportunity, commitment, capacity, scheduling, cancellation, no-show, team, trust, facility, safety, and history rules.

### Phase 5
The entity separation is represented in persistence: users, player profiles, teams, memberships, opportunities, requests, scheduled games, participants, proposals/changes, attendance, results, disputes, trust evidence/assessments, facilities, blocks/reports, notifications, messages, and historical/domain events remain distinct concepts.

### Phase 6
The implementation follows the modular-monolith Kotlin/Spring/PostgreSQL architecture, uses Flyway migrations, backend authority, transactional services, durable domain events, security boundaries, and asynchronous processing adapters.

### Phase 7
The implementation includes domain-oriented REST operations, structured errors, authentication/authorization, idempotency, cursor-ready deterministic discovery ordering, lifecycle operations, scheduling/material-change workflows, attendance/results/disputes, trust, communication, facilities, and moderation primitives.

### Phase 8
The backend contract preserves the Android baseline: backend authority, explicit lifecycle states, critical-write confirmation, no client-side truth creation, notification-as-signal semantics, privacy-aware public data, team authority, and domain separation.

### Phase 9A–9K
All previously locked decisions remain the implementation constraints. No earlier phase was reopened. The only implementation correction was the forward database migration described above; it fixes an implementation/schema contradiction without changing product behavior.

## Final gate still required

Run in CI/staging with dependency and PostgreSQL access:

1. `gradle clean test bootJar --no-daemon`
2. Flyway migration against a clean PostgreSQL/PostGIS database.
3. Integration tests for transactional acceptance/capacity races.
4. Integration tests for PostgreSQL overlap exclusion.
5. Firebase Admin ID-token authentication tests.
6. API contract/OpenAPI validation.
7. Failure-injection tests for retries, duplicate idempotency keys, worker failure, stale state, and DB contention.
8. PostgreSQL backup/restore and migration rollback/forward-compatibility checks.

Until that gate passes, **Phase 9L remains active and Phase 9 is not marked complete**.
