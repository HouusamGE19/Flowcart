# Performance Results

This file is intentionally a **results template**, not fabricated benchmark data.
Run the included k6 scenarios on a documented machine/environment, then replace the placeholders below with real measurements.

## Environment

- Date:
- Git commit:
- Machine / cloud instance:
- CPU:
- RAM:
- OS:
- Docker version:
- Java version:
- Kafka configuration:
- PostgreSQL configuration:

## Order throughput

Command:

```bash
RATE=25 DURATION=2m k6 run performance-tests/k6/order-throughput.js
```

| Arrival rate | Duration | HTTP p95 | HTTP p99 | End-to-end p95 | Failed orders | Notes |
|---:|---:|---:|---:|---:|---:|---|
| TBD | TBD | TBD | TBD | TBD | TBD | Run the test and record real values |

## SSE concurrency

Command:

```bash
VUS=500 DURATION=2m k6 run performance-tests/k6/sse-connections.js
```

| Concurrent clients | Duration | First-event p95 | Connection failures | CPU peak | Heap peak | Notes |
|---:|---:|---:|---:|---:|---:|---|
| 100 | 2m | TBD | TBD | TBD | TBD | baseline |
| 500 | 5m | TBD | TBD | TBD | TBD | |
| 1,000 | 10m | TBD | TBD | TBD | TBD | |
| 2,500 | 10m | TBD | TBD | TBD | TBD | |
| 5,000 | 10m | TBD | TBD | TBD | TBD | saturation exploration |

## Interpretation

Document the first bottleneck you observe (application threads, DB pool, Kafka lag, CPU, memory, network, or client limits), the change you tested, and whether the measurement improved. A short honest analysis is more valuable than a large unexplained number.
