# Architecture

FlowCart is deliberately split into small services that demonstrate two communication styles:

- **Synchronous read dependency:** `order-service` asks `catalog-service` for authoritative product pricing before accepting an order.
- **Asynchronous state changes:** order fulfillment is choreographed through Kafka events. No service reaches into another service's database.

```mermaid
flowchart LR
    UI[Angular Operations UI] -->|REST| CAT[Catalog Service]
    UI -->|REST + SSE| ORD[Order Service]
    ORD -->|GET product price| CAT
    ORD -->|Transactional Outbox| K[(Kafka: flowcart.events)]
    K --> INV[Inventory Service]
    INV -->|Outbox| K
    K --> PAY[Payment Service]
    PAY -->|Outbox| K
    K --> ORD
    K --> NOTIF[Notification Service]
    ORD -->|SSE| UI

    CAT --> PG[(PostgreSQL)]
    ORD --> PG
    INV --> PG
    PAY --> PG
    NOTIF --> PG
```

## Service boundaries

| Service | Responsibility | Port | Data ownership |
|---|---|---:|---|
| catalog-service | Product query/pricing | 8081 | `catalog` schema |
| order-service | Order aggregate, saga state, SSE projection | 8082 | `orders` schema |
| inventory-service | Stock reservation and compensation | 8083 | `inventory` schema |
| payment-service | Deterministic payment simulator | 8084 | `payment` schema |
| notification-service | Idempotent simulated email delivery | 8085 | `notification` schema |

## Reliability model

The design assumes Kafka may redeliver. Every state-changing consumer stores the source `eventId` in a table with a primary-key/unique constraint before considering the event processed. This makes duplicate delivery safe.

Every service that emits state-changing events uses a **transactional outbox**. The domain update and outbox row are committed in the same PostgreSQL transaction. A scheduled publisher forwards unpublished rows to Kafka and only then marks them published. If the process dies after Kafka accepted the event but before `published_at` is stored, the event may be sent again; idempotent consumers absorb that duplicate.

## Failure path

```mermaid
sequenceDiagram
    participant C as Client
    participant O as Order
    participant K as Kafka
    participant I as Inventory
    participant P as Payment
    participant N as Notification

    C->>O: POST /api/orders (simulatePaymentFailure=true)
    O->>O: save order + OrderCreated outbox
    O-->>K: OrderCreated
    K-->>I: OrderCreated
    I->>I: reserve stock + InventoryReserved outbox
    I-->>K: InventoryReserved
    K-->>P: InventoryReserved
    P->>P: save FAILED payment + PaymentFailed outbox
    P-->>K: PaymentFailed
    par inventory compensation
        K-->>I: PaymentFailed
        I->>I: release reserved stock
        I-->>K: InventoryReleased
    and order cancellation
        K-->>O: PaymentFailed
        O->>O: order -> CANCELLED + OrderCancelled outbox
        O-->>K: OrderCancelled
        K-->>N: OrderCancelled
    end
    O-->>C: live SSE events
```

The two branches after `PaymentFailed` are independent consumer groups. Their relative completion order is not guaranteed; correctness depends on both side effects being idempotent, not on a global ordering between services.

## Deliberate tradeoffs

This portfolio version uses one Kafka topic keyed by order ID so all events for a given order remain in partition order. In a larger system, event schemas would be versioned (often via Avro/Protobuf + schema registry), dead-letter handling would be explicit, and the outbox publisher would normally use CDC (for example Debezium) rather than a polling scheduler.
