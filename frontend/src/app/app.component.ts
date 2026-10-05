import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EventConsoleComponent } from './event-console/event-console.component';
import { InventoryItem, Order, Product, ProductPresentation, TimelineEvent, ViewName } from './models';

const PRODUCT_PRESENTATION: Record<string, ProductPresentation> = {
  'AJ1-OW-CHICAGO-AA3834-101': {
    accent: '#ff1b1b', index: '01', edition: 'THE TEN / 2017', kicker: 'CHICAGO',
    heroLeft: 'AIR', heroRight: 'ONE', eyebrow: 'OFF-WHITE × JORDAN'
  },
  'AJ4-FIRERED-DC7770-160': {
    accent: '#f01822', index: '02', edition: 'RETRO / 2020', kicker: 'FIRE RED',
    heroLeft: 'FIRE', heroRight: 'RED', eyebrow: 'AIR JORDAN 4'
  },
  'AZG-FIRSTGAME-941911-100': {
    accent: '#ba1225', index: '03', edition: 'FIRST GAME', kicker: 'CRIMSON / NAVY',
    heroLeft: 'AIR ZOOM', heroRight: 'GENERATION', eyebrow: 'NIKE BASKETBALL'
  },
  'AJ11-BRED-378037-061': {
    accent: '#e31625', index: '04', edition: 'PLAYOFFS / 2019', kicker: 'BRED',
    heroLeft: 'PLAY', heroRight: 'OFFS', eyebrow: 'AIR JORDAN 11'
  },
  'PKDM-BW-898033-001': {
    accent: '#f0f0f0', index: '05', edition: 'DM / 2017', kicker: 'BLACK / WHITE',
    heroLeft: 'POCKET', heroRight: 'KNIFE', eyebrow: 'NIKE SPORTSWEAR'
  }
};

const FALLBACK_PRESENTATION: ProductPresentation = {
  accent: '#f31319', index: '--', edition: 'FLOWCART', kicker: 'SNEAKERS',
  heroLeft: 'FLOW', heroRight: 'CART', eyebrow: 'EVENT-DRIVEN COMMERCE'
};

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, EventConsoleComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  // Browser calls stay same-origin. In local dev Angular proxies /api to :8086;
  // in Docker/production Nginx proxies /api to gateway-service:8086.
  private readonly apiBase = '';
  private readonly catalogApi = this.apiBase;
  private readonly orderApi = this.apiBase;
  private readonly inventoryApi = this.apiBase;
  private source?: EventSource;

  readonly view = signal<ViewName>('home');
  readonly products = signal<Product[]>([]);
  readonly orders = signal<Order[]>([]);
  readonly inventory = signal<InventoryItem[]>([]);
  readonly order = signal<Order | null>(null);
  readonly events = signal<TimelineEvent[]>([]);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly checkoutOpen = signal(false);
  readonly consoleVisible = signal(false);

  email = 'engineer@example.com';
  readonly selectedSku = signal('');
  quantity = 1;
  simulatePaymentFailure = false;

  readonly selectedProduct = computed(() => this.products().find(product => product.sku === this.selectedSku()) ?? null);
  readonly homeProducts = computed(() => {
    const products = this.products();
    if (products.length <= 3) return products;

    const featured = products.slice(0, 3);
    const selected = products.find(product => product.sku === this.selectedSku());
    if (!selected || featured.some(product => product.sku === selected.sku)) return featured;

    // Keep a product selected from SHOP visible when the user returns HOME.
    return [featured[0], featured[1], selected];
  });
  readonly selectedMeta = computed(() => this.metaForSku(this.selectedSku()));
  readonly selectedInventory = computed(() => this.inventory().find(item => item.sku === this.selectedSku()) ?? null);
  readonly statusLabel = computed(() => this.order()?.status ?? 'READY');
  readonly orderNumber = computed(() => this.order()?.id.slice(0, 8).toUpperCase() ?? 'NO ACTIVE ORDER');
  readonly totalReserved = computed(() => this.inventory().reduce((sum, item) => sum + item.reservedQuantity, 0));
  readonly totalAvailable = computed(() => this.inventory().reduce((sum, item) => sum + item.availableQuantity, 0));

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.loadProducts();
    this.loadOrders();
    this.loadInventory();
  }

  retryCatalog(): void {
    this.error.set('');
    this.loadProducts();
    this.loadOrders();
    this.loadInventory();
  }

  setView(view: ViewName): void {
    this.view.set(view);
    this.error.set('');
    if (view === 'orders') this.loadOrders();
    if (view === 'inventory') this.loadInventory();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  selectProduct(sku: string): void {
    this.selectedSku.set(sku);
    this.setView('home');
  }

  orderProduct(sku: string): void {
    this.selectedSku.set(sku);
    this.simulatePaymentFailure = false;
    this.error.set('');
    this.checkoutOpen.set(true);
  }

  openCheckout(simulateFailure = false): void {
    if (!this.selectedProduct()) return;
    this.simulatePaymentFailure = simulateFailure;
    this.checkoutOpen.set(true);
    this.error.set('');
  }

  closeCheckout(): void {
    if (!this.busy()) this.checkoutOpen.set(false);
  }

  createOrder(): void {
    const product = this.selectedProduct();
    if (!product) return;

    this.busy.set(true);
    this.error.set('');
    this.events.set([]);
    this.source?.close();

    const body = {
      customerEmail: this.email,
      items: [{ sku: product.sku, quantity: Number(this.quantity) }],
      simulatePaymentFailure: this.simulatePaymentFailure
    };

    this.http.post<Order>(`${this.orderApi}/api/orders`, body).subscribe({
      next: order => {
        this.order.set(order);
        this.busy.set(false);
        this.checkoutOpen.set(false);
        this.consoleVisible.set(true);
        this.connect(order.id);
        this.loadOrders();
      },
      error: err => {
        this.busy.set(false);
        this.error.set(err?.error?.detail ?? 'Could not create order. Check that the backend services are running.');
      }
    });
  }

  inspectOrder(order: Order): void {
    this.order.set(order);
    const sku = order.items[0]?.sku;
    if (sku && this.products().some(product => product.sku === sku)) this.selectedSku.set(sku);
    this.events.set([]);
    this.consoleVisible.set(true);
    this.connect(order.id);
  }

  metaFor(product: Product): ProductPresentation {
    return this.metaForSku(product.sku);
  }

  metaForSku(sku: string): ProductPresentation {
    return PRODUCT_PRESENTATION[sku] ?? FALLBACK_PRESENTATION;
  }


  productImage(product: Product): string {
    return product.imageUrl || '';
  }

  inventoryFor(sku: string): InventoryItem | undefined {
    return this.inventory().find(item => item.sku === sku);
  }

  productForSku(sku: string): Product | undefined {
    return this.products().find(product => product.sku === sku);
  }

  stockPercent(stock: InventoryItem): number {
    const total = stock.availableQuantity + stock.reservedQuantity;
    return total > 0 ? (stock.availableQuantity / total) * 100 : 0;
  }


  onProductImageError(event: Event, sku: string): void {
    const image = event.target as HTMLImageElement;
    console.warn(`Product image failed for ${sku}: ${image.src}`);
    image.style.visibility = 'hidden';
  }

  onShopPointerMove(event: MouseEvent): void {
    const card = event.currentTarget as HTMLElement | null;
    if (!card) return;

    const rect = card.getBoundingClientRect();
    const x = Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width));
    const y = Math.max(0, Math.min(1, (event.clientY - rect.top) / rect.height));

    card.style.setProperty('--pointer-x', `${x * 100}%`);
    card.style.setProperty('--pointer-y', `${y * 100}%`);
    card.style.setProperty('--parallax-x', `${(x - 0.5) * 24}px`);
    card.style.setProperty('--parallax-y', `${(y - 0.5) * 14}px`);
  }

  resetShopPointer(event: MouseEvent): void {
    const card = event.currentTarget as HTMLElement | null;
    if (!card) return;
    card.style.setProperty('--pointer-x', '50%');
    card.style.setProperty('--pointer-y', '50%');
    card.style.setProperty('--parallax-x', '0px');
    card.style.setProperty('--parallax-y', '0px');
  }

  shopTitleLines(product: Product): string[] {
    switch (product.sku) {
      case 'AJ1-OW-CHICAGO-AA3834-101':
        return ['AIR JORDAN 1', 'OFF-WHITE CHICAGO'];
      case 'AJ4-FIRERED-DC7770-160':
        return ['AIR JORDAN 4', 'FIRE RED'];
      case 'AZG-FIRSTGAME-941911-100':
        return ['AIR ZOOM', 'GENERATION'];
      case 'AJ11-BRED-378037-061':
        return ['AIR JORDAN 11', 'PLAYOFFS BRED'];
      case 'PKDM-BW-898033-001':
        return ['POCKET KNIFE', 'DM'];
      default:
        return [product.name];
    }
  }

  dropIndex(index: number): string {
    return String(index + 1).padStart(2, '0');
  }

  money(cents: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(cents / 100);
  }

  shortId(id: string): string {
    return id.slice(0, 8).toUpperCase();
  }

  statusTone(status: string): string {
    if (status === 'CONFIRMED') return 'success';
    if (status === 'CANCELLED') return 'danger';
    if (status === 'INVENTORY_RESERVED') return 'progress';
    return 'neutral';
  }

  private loadProducts(): void {
    this.http.get<Product[]>(`${this.catalogApi}/api/products`).subscribe({
      next: data => {
        const active = data.filter(product => product.active);
        this.products.set(active);
        if (active.length && !active.some(product => product.sku === this.selectedSku())) {
          this.selectedSku.set(active.find(product => product.sku === 'AJ1-OW-CHICAGO-AA3834-101')?.sku ?? active[0].sku);
        }
      },
      error: err => {
        console.error('Catalog request failed', err);
        this.error.set('CATALOG API UNAVAILABLE — CHECK GATEWAY :8086');
      }
    });
  }

  private loadOrders(): void {
    this.http.get<Order[]>(`${this.orderApi}/api/orders`).subscribe({
      next: data => this.orders.set(data),
      error: () => undefined
    });
  }

  private loadInventory(): void {
    this.http.get<InventoryItem[]>(`${this.inventoryApi}/api/inventory`).subscribe({
      next: data => this.inventory.set(data),
      error: () => undefined
    });
  }

  private connect(orderId: string): void {
    this.source?.close();
    this.source = new EventSource(`${this.orderApi}/api/orders/${orderId}/events`);
    const eventNames = ['snapshot', 'InventoryReserved', 'InventoryRejected', 'PaymentCompleted', 'PaymentFailed', 'InventoryReleased'];

    for (const name of eventNames) {
      this.source.addEventListener(name, (event: MessageEvent) => {
        let detail = name;
        try {
          const data = JSON.parse(event.data) as { status?: string; type?: string; data?: { reason?: string; sku?: string } };
          detail = data.data?.reason ?? data.status ?? data.data?.sku ?? data.type ?? name;
        } catch {
          detail = name;
        }

        this.events.update(events => [{
          at: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
          name,
          detail,
          service: this.serviceFor(name)
        }, ...events].slice(0, 40));
        this.refresh(orderId);
      });
    }

    this.source.onerror = () => this.events.update(events => [{
      at: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
      name: 'SSE_RECONNECT',
      detail: 'Connection interrupted; the browser will retry automatically.',
      service: 'order-service'
    }, ...events].slice(0, 40));
  }

  private serviceFor(eventName: string): string {
    if (eventName.startsWith('Inventory')) return 'inventory-service';
    if (eventName.startsWith('Payment')) return 'payment-service';
    return 'order-service';
  }

  private refresh(orderId: string): void {
    this.http.get<Order>(`${this.orderApi}/api/orders/${orderId}`).subscribe({
      next: order => {
        this.order.set(order);
        this.loadOrders();
        this.loadInventory();
      },
      error: () => undefined
    });
  }

  ngOnDestroy(): void {
    this.source?.close();
  }
}
