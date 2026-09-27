# Distributed Job Processing System

This project demonstrates asynchronous and concurrent job processing with Spring Boot, PostgreSQL, Redis, and Maven.

## Problem

The API accepts work without making clients wait for long-running report, email, or image operations. Jobs have durable state, retries, failure visibility, and recovery when a worker stops unexpectedly.

## Architecture

```text
Client -> Spring Boot REST API
             |              \
             v               v
       PostgreSQL        Redis ready queue
       job state/history       |
                               v
                       Redis processing queue
                               |
                               v
                         Worker pool (4)
                               |
                               v
                          JobHandler strategy
                        /          |          \
                   Report        Email       Image
```

PostgreSQL is the durable source of truth for job state and history. Redis is the fast dispatch layer. Workers provide asynchronous concurrent execution. `JobHandler` makes job types pluggable without changing the worker loop.

## Job lifecycle

```text
QUEUED -> PROCESSING -> COMPLETED
                   \
                    -> RETRYING -> QUEUED
                   \
                    -> FAILED (after max attempts)
```

The default maximum is three attempts. Retry delays are exponential: with a one-second base delay, retries wait one and then two seconds. Configure `MAX_ATTEMPTS` and `RETRY_BASE_DELAY_MS`.

## Redis reliability and duplicate delivery

New IDs are appended to `job_queue`. A worker atomically moves an ID to `job_queue:processing` before loading the job. Success or terminal failure removes the ID from the processing list.

A scheduled recovery scan requeues processing entries whose persisted job has been in `PROCESSING` longer than `RECOVERY_TIMEOUT_MS`. A worker crash after claiming a job therefore does not permanently lose it.

This is intentionally an at-least-once design. A slow worker can exceed the visibility timeout and be delivered again. The database claim update only allows `QUEUED` or `RETRYING` jobs to become `PROCESSING`; duplicate deliveries of completed or currently claimed jobs are skipped. This reduces duplicate work but does not provide exactly-once execution. Real side effects should be idempotent or use an idempotency key.

## API

Create a job:

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{"type":"EMAIL","payload":"{}"}'
```

Supported types are `REPORT_GENERATION`, `EMAIL`, and `IMAGE_PROCESSING`.

Fetch job state:

```bash
curl http://localhost:8080/api/jobs/<job-id>
```

Invalid requests return `400`; an unknown job ID returns `404` without exposing a stack trace.

## Run locally

Requirements: Java 21+, Docker, and Maven Wrapper.

```bash
docker compose up -d
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
./mvnw spring-boot:run
```

The application defaults to `localhost:5432` and `localhost:6379`. Configurable values include `WORKER_COUNT`, `MAX_ATTEMPTS`, `RETRY_BASE_DELAY_MS`, `RECOVERY_INTERVAL_MS`, and `RECOVERY_TIMEOUT_MS`.

Stop infrastructure with `docker compose down`. Add `-v` only when you intentionally want to remove the local PostgreSQL volume.

## Observability

Spring Boot Actuator exposes `/actuator/health` and `/actuator/metrics`. The application records submitted, completed, failed, retried, currently processing, and processing-duration metrics. Worker logs include worker name, job ID, type, attempt, retry, completion, and failure events.

## Testing

```bash
./mvnw clean compile
./mvnw test
```

Tests cover job creation, successful execution, retry and terminal failure behavior, duplicate delivery protection, and API validation/not-found responses. The context test uses H2 and disables external workers; normal development uses PostgreSQL and Redis.

## Known limitations

- The ready-to-processing move and PostgreSQL state update are not one distributed transaction. The persisted claim and recovery process provide practical recovery, not a global transaction.
- A visibility timeout can cause duplicate execution if a handler runs longer than the configured timeout.
- Retry backoff sleeps in the worker thread, consuming a worker slot during the delay.
- `ddl-auto=update` is convenient here; a larger production system should use Flyway or Liquibase migrations.
- Sample handlers sleep to simulate work and do not call real external services.

## Future improvements

Redis Streams with consumer groups and pending-entry inspection would provide richer acknowledgement and recovery semantics. A transactional outbox could close the database-to-queue submission gap. Further improvements could add per-handler timeouts, structured JSON logs, authentication, and containerized PostgreSQL/Redis integration tests.
