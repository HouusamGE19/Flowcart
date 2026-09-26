# ADR-002: Choreographed saga for fulfillment

**Status:** Accepted

## Context

A single ACID transaction cannot span order, inventory and payment databases without tightly coupling the services.

## Decision

Use Kafka events as a choreographed saga. Inventory reacts to `OrderCreated`; payment reacts to `InventoryReserved`; order reacts to fulfillment results. On `PaymentFailed`, inventory performs a compensating release.

## Consequences

- Services remain independently owned and deployable.
- Failure recovery is visible as business events.
- The flow is eventually consistent.
- Choreography becomes harder to reason about as the number of steps grows. A dedicated orchestrator would be considered for a larger saga.
