# Enterprise Order & Fulfillment Platform

A standalone three-tier commerce application designed to create realistic telemetry for APM, distributed tracing, database monitoring, logs, metrics, and error tracking. It includes a Next.js browser UI, a Java 8/Spring Boot API, PostgreSQL schema/seed data, mock HTTP payment and shipping services, and diagnostic transactions.

## Architecture

```text
Browser → Next.js (3000) → Spring Boot API (8080) → JDBC → PostgreSQL (5432)
                                  └→ HTTP → mock payment / mock shipping
```

## Prerequisites

This project deliberately requires **Java 8** (not newer), Maven 3.6+, Node.js 18+, npm, PostgreSQL 12+, `psql`, and `curl`. Verify each with `java -version`, `mvn -version`, `node -v`, `npm -v`, and `psql --version`.

## First-time setup

1. `cp .env.example .env` and edit database credentials if needed.
2. Create the database and user in PostgreSQL. The user needs permission to create tables.
3. Run `./scripts/setup.sh`, then `./scripts/seed-db.sh`.
4. Start everything with `./scripts/start.sh`.
5. Open http://localhost:3000 and verify with `./scripts/health-check.sh`.

Configuration is simple: shell environment variables override `.env`, which overrides defaults in `backend/src/main/resources/application.yml`. `.env` is intentionally ignored by Git. Important controls include `DIAGNOSTIC_DELAY_MS`, external service latency/failure rates, background intervals, and `ENABLE_SLOW_DB_TESTS` (false by default).

Use `scripts/status.sh`, `scripts/logs.sh`, `scripts/restart.sh`, and `scripts/stop.sh` for routine operation. `scripts/test.sh` runs automated backend tests. `scripts/generate-test-traffic.sh` produces browsing, search, order lookup, slow, and controlled-error traffic.

## API highlights

`GET /api/v1/products`, `/customers`, `/orders/{id}`, `/dashboard`, `/operations`; `POST /api/v1/orders`; `/actuator/health`; and application health at `/api/v1/health`. Every API response is JSON and application errors use a consistent timestamp/status/error/message/requestId/path structure. Incoming `X-Request-ID` and `X-Correlation-ID` are retained or generated, returned to callers, logged through MDC, and intended for propagation to external HTTP calls.

## APM test matrix

| Scenario | Endpoint | Expected observation |
| --- | --- | --- |
| Normal API | `/api/v1/diagnostics/normal` | Normal transaction |
| Slow API | `/api/v1/diagnostics/slow` | High response time |
| Server error | `/api/v1/diagnostics/server-error` | Error |
| DB error | `/api/v1/diagnostics/database-error` | DB exception |
| DB slow query | `/api/v1/diagnostics/database/slow-query` | DB latency |
| Payment failure | `/api/v1/diagnostics/external-service-error` | External failure |
| Timeout | `/api/v1/diagnostics/timeout` | Timeout |
| Order workflow | `POST /api/v1/orders` | Transactional business flow |
| Product search | `/api/v1/products` | Indexed DB query |
| Background jobs | Scheduler | Background activity |

Diagnostics require `ENABLE_DIAGNOSTIC_ENDPOINTS=true`; the expensive cross-join route additionally requires `ENABLE_SLOW_DB_TESTS=true` and is deliberately disabled by default. Do not enable it in a production database.

## Instrumentation

Instrument the Java process with your chosen Java agent (AppDynamics, Dynatrace, New Relic, Elastic, or OpenTelemetry); add browser/RUM instrumentation to Next.js; monitor PostgreSQL; and add host infrastructure monitoring. The app is vendor-neutral: Actuator health and Micrometer metrics are exposed without credentials, and no vendor keys are embedded.
