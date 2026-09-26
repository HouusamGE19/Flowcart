# ADR-001: Transactional outbox for event publication

**Status:** Accepted

## Context

Saving an order and publishing `OrderCreated` are two writes to different systems. A naive implementation can save the database row and then crash before Kafka accepts the message, leaving an order that can never progress.

## Decision

Persist the domain state and a serialized event in an `outbox_events` table in one database transaction. A scheduler publishes pending rows to Kafka and records `published_at` only after the broker acknowledges the send.

## Consequences

- A database commit cannot lose the intent to publish.
- Publication is retryable.
- A crash after Kafka acknowledgment but before `published_at` can create duplicates.
- Therefore every state-changing Kafka consumer must be idempotent by `eventId`.
- Polling is simple and visible for a portfolio project; CDC would be preferable at larger scale.
