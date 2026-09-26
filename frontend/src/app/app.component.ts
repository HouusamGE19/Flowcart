import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface Product { id: string; sku: string; name: string; priceCents: number; active: boolean; }
interface OrderItem { sku: string; quantity: number; unitPriceCents: number; }
interface Order { id: string; customerEmail: string; status: string; totalCents: number; items: OrderItem[]; }
interface TimelineEvent { at: string; name: string; detail: string; }

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly host = window.location.hostname || 'localhost';
  private readonly catalogApi = `http://${this.host}:8081`;
  private readonly orderApi = `http://${this.host}:8082`;
  private source?: EventSource;

  readonly products = signal<Product[]>([]);
  readonly order = signal<Order | null>(null);
  readonly events = signal<TimelineEvent[]>([]);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly statusLabel = computed(() => this.order()?.status ?? 'NO ORDER');

  email = 'engineer@example.com';
  selectedSku = 'LAPTOP-PRO-14';
  quantity = 1;
  simulatePaymentFailure = false;

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<Product[]>(`${this.catalogApi}/api/products`).subscribe({
      next: data => {
        this.products.set(data);
        if (data.length && !data.some(p => p.sku === this.selectedSku)) this.selectedSku = data[0].sku;
      },
      error: () => this.error.set('Catalog is unavailable. Start the Docker Compose stack first.')
    });
  }

  createOrder(): void {
    this.busy.set(true);
    this.error.set('');
    this.events.set([]);
    this.source?.close();
    const body = {
      customerEmail: this.email,
      items: [{ sku: this.selectedSku, quantity: Number(this.quantity) }],
      simulatePaymentFailure: this.simulatePaymentFailure
    };
    this.http.post<Order>(`${this.orderApi}/api/orders`, body).subscribe({
      next: order => {
        this.order.set(order);
        this.busy.set(false);
        this.connect(order.id);
      },
      error: err => {
        this.busy.set(false);
        this.error.set(err?.error?.detail ?? 'Could not create order.');
      }
    });
  }

  private connect(orderId: string): void {
    this.source = new EventSource(`${this.orderApi}/api/orders/${orderId}/events`);
    const eventNames = ['snapshot','InventoryReserved','InventoryRejected','PaymentCompleted','PaymentFailed','InventoryReleased'];
    for (const name of eventNames) {
      this.source.addEventListener(name, (event: MessageEvent) => {
        let detail = '';
        try {
          const data = JSON.parse(event.data);
          detail = data.data?.reason ?? data.status ?? data.type ?? name;
        } catch { detail = name; }
        this.events.update(events => [{ at: new Date().toLocaleTimeString(), name, detail }, ...events].slice(0, 20));
        this.refresh(orderId);
      });
    }
    this.source.onerror = () => this.events.update(events => [{ at: new Date().toLocaleTimeString(), name: 'SSE reconnect', detail: 'Connection interrupted; browser will retry.' }, ...events].slice(0, 20));
  }

  private refresh(orderId: string): void {
    this.http.get<Order>(`${this.orderApi}/api/orders/${orderId}`).subscribe(order => this.order.set(order));
  }

  money(cents: number): string { return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(cents / 100); }
  ngOnDestroy(): void { this.source?.close(); }
}
