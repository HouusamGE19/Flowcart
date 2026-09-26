package dev.houssam.flowcart.inventory.service;

import dev.houssam.flowcart.events.DomainEvent;
import dev.houssam.flowcart.events.EventData;
import dev.houssam.flowcart.events.EventTypes;
import dev.houssam.flowcart.inventory.domain.InventoryItem;
import dev.houssam.flowcart.inventory.domain.InventoryRepository;
import dev.houssam.flowcart.inventory.events.ProcessedEvent;
import dev.houssam.flowcart.inventory.events.ProcessedEventRepository;
import dev.houssam.flowcart.inventory.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryEventHandler {
    private final InventoryRepository inventory;
    private final ProcessedEventRepository processed;
    private final OutboxService outbox;

    public InventoryEventHandler(InventoryRepository inventory, ProcessedEventRepository processed, OutboxService outbox) {
        this.inventory = inventory; this.processed = processed; this.outbox = outbox;
    }

    @Transactional
    public void handle(DomainEvent event) {
        if (processed.existsById(event.eventId())) return;

        switch (event.type()) {
            case EventTypes.ORDER_CREATED -> reserve(event);
            case EventTypes.PAYMENT_FAILED -> release(event);
            default -> { return; }
        }
        processed.save(new ProcessedEvent(event.eventId()));
    }

    private void reserve(DomainEvent event) {
        List<Map<String, Object>> lines = EventData.items(event.data());
        String unavailableSku = null;
        for (Map<String, Object> line : lines) {
            String sku = EventData.string(line, "sku");
            int quantity = Math.toIntExact(EventData.longValue(line, "quantity"));
            InventoryItem item = inventory.findById(sku).orElse(null);
            if (item == null || !item.canReserve(quantity)) {
                unavailableSku = sku;
                break;
            }
        }

        Map<String, Object> data = new LinkedHashMap<>(event.data());
        if (unavailableSku != null) {
            data.put("reason", "Insufficient stock for " + unavailableSku);
            outbox.add(DomainEvent.of(EventTypes.INVENTORY_REJECTED, event.aggregateId(), data));
            return;
        }

        for (Map<String, Object> line : lines) {
            String sku = EventData.string(line, "sku");
            int quantity = Math.toIntExact(EventData.longValue(line, "quantity"));
            inventory.findById(sku).orElseThrow().reserve(quantity);
        }
        outbox.add(DomainEvent.of(EventTypes.INVENTORY_RESERVED, event.aggregateId(), data));
    }

    private void release(DomainEvent event) {
        for (Map<String, Object> line : EventData.items(event.data())) {
            String sku = EventData.string(line, "sku");
            int quantity = Math.toIntExact(EventData.longValue(line, "quantity"));
            inventory.findById(sku).ifPresent(item -> item.release(quantity));
        }
        Map<String, Object> data = new LinkedHashMap<>(event.data());
        data.put("reason", "Payment failed; reservation compensated");
        outbox.add(DomainEvent.of(EventTypes.INVENTORY_RELEASED, event.aggregateId(), data));
    }
}
