# Event Contract

All services share the lightweight `DomainEvent` envelope from `common-events`.

```json
{
  "eventId": "3b6dcb57-...",
  "type": "InventoryReserved",
  "aggregateId": "7ab7dc6e-...",
  "occurredAt": "2026-09-24T12:00:00Z",
  "data": {
    "customerEmail": "engineer@example.com",
    "totalCents": 24900,
    "simulatePaymentFailure": false,
    "items": [{"sku": "HEADSET-NC-01", "quantity": 1, "unitPriceCents": 24900}]
  }
}
```

## Event flow

| Event | Producer | Consumers | Purpose |
|---|---|---|---|
| `OrderCreated` | order | inventory, order (ignored locally) | Start fulfillment |
| `InventoryReserved` | inventory | payment, order | Stock reserved successfully |
| `InventoryRejected` | inventory | order | Cancel order because stock is unavailable |
| `PaymentCompleted` | payment | order | Confirm order |
| `PaymentFailed` | payment | order, inventory | Cancel order and compensate stock |
| `InventoryReleased` | inventory | order | Visible compensation event |
| `OrderConfirmed` | order | notification | Final success notification |
| `OrderCancelled` | order | notification | Final cancellation notification |

## Delivery semantics

FlowCart intentionally demonstrates **at-least-once delivery** rather than pretending Kafka is exactly-once end to end. An outbox row can be published twice after a crash, so consumers deduplicate by `eventId`. This is a useful failure mode to discuss in interviews because it shows how database/Kafka boundaries are handled in practice.
