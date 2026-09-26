package dev.houssam.flowcart.order.service;

import dev.houssam.flowcart.events.DomainEvent;
import dev.houssam.flowcart.events.EventTypes;
import dev.houssam.flowcart.order.api.CreateOrderRequest;
import dev.houssam.flowcart.order.api.OrderResponse;
import dev.houssam.flowcart.order.catalog.CatalogClient;
import dev.houssam.flowcart.order.catalog.CatalogProduct;
import dev.houssam.flowcart.order.domain.OrderEntity;
import dev.houssam.flowcart.order.domain.OrderRepository;
import dev.houssam.flowcart.order.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderApplicationService {
    private final OrderRepository orders;
    private final CatalogClient catalog;
    private final OutboxService outbox;

    public OrderApplicationService(OrderRepository orders, CatalogClient catalog, OutboxService outbox) {
        this.orders = orders;
        this.catalog = catalog;
        this.outbox = outbox;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        record PricedItem(String sku, int quantity, long unitPriceCents) {}

        List<PricedItem> priced = request.items().stream().map(item -> {
            CatalogProduct product = catalog.requireProduct(item.sku());
            return new PricedItem(product.sku(), item.quantity(), product.priceCents());
        }).toList();

        long total = priced.stream()
                .mapToLong(item -> Math.multiplyExact(item.unitPriceCents(), item.quantity()))
                .reduce(0L, Math::addExact);

        OrderEntity order = new OrderEntity(UUID.randomUUID(), request.customerEmail(), total, request.simulatePaymentFailure());
        priced.forEach(item -> order.addItem(item.sku(), item.quantity(), item.unitPriceCents()));
        orders.save(order);

        List<Map<String, Object>> items = priced.stream().map(item -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("sku", item.sku());
            data.put("quantity", item.quantity());
            data.put("unitPriceCents", item.unitPriceCents());
            return data;
        }).toList();

        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("customerEmail", request.customerEmail());
        eventData.put("totalCents", total);
        eventData.put("simulatePaymentFailure", request.simulatePaymentFailure());
        eventData.put("items", items);
        outbox.add(DomainEvent.of(EventTypes.ORDER_CREATED, order.getId(), eventData));

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id) {
        return orders.findById(id).map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> recent() {
        return orders.findTop50ByOrderByCreatedAtDesc().stream().map(OrderResponse::from).toList();
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(UUID id) { super("Order not found: " + id); }
    }
}
