# Architecture

FlowCart is deliberately split into small services that demonstrate two communication styles:

- **Synchronous read dependency:** `order-service` asks `catalog-service` for authoritative product pricing before accepting an order.
- **Asynchronous state changes:** order fulfillment is choreographed through Kafka events. No service reaches into another service's database.

```mermaid
flowchart LR
    UI[Angular Operations UI] -->|REST + SSE| GW[Spring Cloud Gateway]
    GW -->|lb://catalog-service| CAT[Catalog Service]
    GW -->|lb://order-service| ORD[Order Service]
    GW -->|lb://inventory-service| INV[Inventory Service]
    GW -->|lb://payment-service| PAY[Payment Service]
    GW -->|lb://notification-service| NOTIF[Notification Service]

    CONSUL[(Consul Registry)]
    GW -. discovery .-> CONSUL
    CAT -. register .-> CONSUL
    ORD -. register .-> CONSUL
    INV -. register .-> CONSUL
    PAY -. register .-> CONSUL
    NOTIF -. register .-> CONSUL

    ORD -->|load-balanced GET product price| CAT
    ORD -->|Transactional Outbox| K[(Kafka: flowcart.events)]
    K --> INV
    INV -->|Outbox| K
    K --> PAY
    PAY -->|Outbox| K
    K --> ORD
    K --> NOTIF

    CAT --> PG[(PostgreSQL)]
    ORD --> PG
    INV --> PG
    PAY --> PG
    NOTIF --> PG
```

## Service boundaries

| Service | Responsibility | Port | Data ownership |
|---|---|---:|---|
| gateway-service | API routing and load-balanced service discovery | 8086 | none |
| catalog-service | Product query/pricing | 8081 | `catalog` schema |
| order-service | Order aggregate, saga state, SSE projection | 8082 | `orders` schema |
| inventory-service | Stock reservation and compensation | 8083 | `inventory` schema |
| payment-service | Deterministic payment simulator | 8084 | `payment` schema |
| notification-service | Idempotent simulated email delivery | 8085 | `notification` schema |

## Service discovery

Each Spring service registers with Consul using its `spring.application.name`. The gateway and
Order Service do not need physical host/port knowledge for discovered HTTP targets:

- Gateway routes use `lb://catalog-service`, `lb://order-service`, and the other logical service IDs.
- Order Service uses a `@LoadBalanced RestClient.Builder` and calls `http://catalog-service`.
- Spring Cloud LoadBalancer asks the Consul-backed `DiscoveryClient` for healthy instances and selects one.

Consul is not involved in asynchronous event delivery. Kafka remains the event backbone.

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
