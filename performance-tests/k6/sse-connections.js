import http from 'k6/http';
import sse from 'k6/x/sse';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE = __ENV.ORDER_BASE_URL || 'http://localhost:8082';
const CATALOG = __ENV.CATALOG_BASE_URL || 'http://localhost:8081';
const clientsOpened = new Counter('sse_clients_opened');
const eventsReceived = new Counter('sse_events_received');
const connectionErrors = new Rate('sse_connection_errors');
const firstEventMs = new Trend('sse_first_event_ms', true);

export const options = {
  scenarios: {
    sustained_connections: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 100),
      duration: __ENV.DURATION || '1m',
      gracefulStop: '5s',
    },
  },
  thresholds: {
    sse_connection_errors: ['rate<0.01'],
    sse_first_event_ms: ['p(95)<1000'],
  },
};

export function setup() {
  const catalog = http.get(`${CATALOG}/api/products`);
  check(catalog, { 'catalog available': r => r.status === 200 });

  const response = http.post(`${BASE}/api/orders`, JSON.stringify({
    customerEmail: 'k6-sse@flowcart.dev',
    items: [{ sku: 'HEADSET-NC-01', quantity: 1 }],
    simulatePaymentFailure: false,
  }), { headers: { 'Content-Type': 'application/json' } });

  check(response, { 'seed order created': r => r.status === 201 });
  return { orderId: response.json('id') };
}

export default function (data) {
  const started = Date.now();
  let firstEvent = true;
  const response = sse.open(`${BASE}/api/orders/${data.orderId}/events`, { method: 'GET' }, client => {
    client.on('open', () => clientsOpened.add(1));
    client.on('event', () => {
      eventsReceived.add(1);
      if (firstEvent) {
        firstEventMs.add(Date.now() - started);
        firstEvent = false;
      }
    });
    client.on('error', () => connectionErrors.add(1));
  });
  check(response, { 'SSE status 200': r => r && r.status === 200 });
}
