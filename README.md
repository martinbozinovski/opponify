# Opponify Backend

Kotlin + Spring Boot modular monolith for the locked Opponify backend specification.

## Architecture

- `app`: API composition, application bootstrap, cross-cutting HTTP concerns.
- `modules/*`: domain/module boundaries.
- PostgreSQL/PostGIS is the target persistence layer.
- Flyway owns schema changes.
- Backend remains authoritative for domain rules.

## Current implementation stage

This repository is the initial executable foundation for Phase 9 implementation. Product/domain rules remain governed by the locked Phases 1–8 and Phase 9 decisions.

Before production use, the remaining domain modules must be implemented and covered by the invariant, integration, concurrency, security and API-contract test suites defined by the roadmap.

## Run

Requires JDK 21 and Gradle (or the Gradle wrapper once generated).

`./gradlew :app:bootRun`

OpenAPI:
`/api/v1/openapi`

Swagger UI:
`/api/v1/docs`

Health:
`/actuator/health`

## Phase 9L implementation notes

This repository contains the concrete Phase 9 backend implementation baseline, not only domain scaffolding.

Implemented backend responsibilities include:
- Firebase JWT boundary with internal-user provisioning and server-side authorization.
- PostgreSQL persistence with Flyway migrations and module/domain boundaries.
- Opportunity creation/discovery/expiration and capacity invariants.
- Participation request lifecycle and atomic acceptance.
- Exact-time scheduling plus range/flexible scheduling finalization.
- Authoritative scheduled-game lifecycle, overlap checks, material-change confirmations, and controlled participant changes.
- Attendance claims/confirmations and safeguarded no-show evidence.
- Result submission/confirmation with sport-specific structural validation for the four launch sports.
- Trust evidence and deterministic recalculation with bounded score, recency, repeated-opponent diminishing contribution, and methodology versioning.
- Team authority, captain/manager controls, and team closure.
- Facilities, suggestions, approved-facility location data, and facility review foundations.
- Blocks, reports, notifications, relationship-scoped messaging, durable domain events, and asynchronous notification dispatch.
- Idempotency-key enforcement for mutations and risk-based API rate limiting.
- Account anonymization that preserves domain/history records.
- Scheduled maintenance for expiration and game-time transitions without manufacturing post-game outcomes.

The build environment used for this workspace does not include Gradle and cannot resolve external package repositories, so full Spring dependency resolution and runtime integration tests must be executed in CI/staging. Pure Kotlin domain compilation and source-level validation are performed locally where dependencies permit.
