import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE = __ENV.ORDER_BASE_URL || 'http://localhost:8082';
const completed = new Rate('orders_reached_terminal_state');
const terminalLatency = new Trend('order_terminal_latency_ms', true);

export const options = {
  scenarios: {
    order_ingestion: {
      executor: 'ramping-arrival-rate',
      startRate: 1,
      timeUnit: '1s',
      preAllocatedVUs: 20,
      maxVUs: 100,
      stages: [
        { target: Number(__ENV.RATE || 10), duration: '20s' },
        { target: Number(__ENV.RATE || 10), duration: __ENV.DURATION || '40s' },
        { target: 0, duration: '10s' },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<750'],
    orders_reached_terminal_state: ['rate>0.98'],
    order_terminal_latency_ms: ['p(95)<5000'],
  },
};

export default function () {
  const started = Date.now();
  const create = http.post(`${BASE}/api/orders`, JSON.stringify({
    customerEmail: `k6-${__VU}-${__ITER}@flowcart.dev`,
    items: [{ sku: 'KEYBOARD-MECH-01', quantity: 1 }],
    simulatePaymentFailure: false,
  }), { headers: { 'Content-Type': 'application/json' } });

  if (!check(create, { 'order accepted': r => r.status === 201 })) {
    completed.add(false);
    return;
  }

  const id = create.json('id');
  for (let i = 0; i < 25; i++) {
    sleep(0.2);
    const get = http.get(`${BASE}/api/orders/${id}`);
    const status = get.json('status');
    if (status === 'CONFIRMED' || status === 'CANCELLED') {
      completed.add(true);
      terminalLatency.add(Date.now() - started);
      return;
    }
  }
  completed.add(false);
}
