package dev.houssam.flowcart.order.service;

import dev.houssam.flowcart.events.DomainEvent;
import dev.houssam.flowcart.events.EventTypes;
import dev.houssam.flowcart.order.domain.OrderEntity;
import dev.houssam.flowcart.order.domain.OrderRepository;
import dev.houssam.flowcart.order.domain.OrderStatus;
import dev.houssam.flowcart.order.events.ProcessedEventEntity;
import dev.houssam.flowcart.order.events.ProcessedEventRepository;
import dev.houssam.flowcart.order.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OrderEventHandler {
    private final ProcessedEventRepository processedEvents;
    private final OrderRepository orders;
    private final OutboxService outbox;

    public OrderEventHandler(ProcessedEventRepository processedEvents, OrderRepository orders, OutboxService outbox) {
        this.processedEvents = processedEvents;
        this.orders = orders;
        this.outbox = outbox;
    }

    @Transactional
    public boolean handle(DomainEvent event) {
        if (processedEvents.existsById(event.eventId())) {
            return false;
        }

        boolean relevant = switch (event.type()) {
            case EventTypes.INVENTORY_RESERVED -> transition(event, OrderStatus.INVENTORY_RESERVED, false);
            case EventTypes.INVENTORY_REJECTED -> transition(event, OrderStatus.CANCELLED, true);
            case EventTypes.PAYMENT_COMPLETED -> transition(event, OrderStatus.CONFIRMED, true);
            case EventTypes.PAYMENT_FAILED -> transition(event, OrderStatus.CANCELLED, true);
            case EventTypes.INVENTORY_RELEASED -> true;
            default -> false;
        };

        if (relevant) {
            processedEvents.save(new ProcessedEventEntity(event.eventId()));
        }
        return relevant;
    }

    private boolean transition(DomainEvent event, OrderStatus status, boolean emitFinalEvent) {
        OrderEntity order = orders.findById(event.aggregateId()).orElse(null);
        if (order == null) {
            return false;
        }
        order.transitionTo(status);

        if (emitFinalEvent) {
            String finalType = status == OrderStatus.CONFIRMED ? EventTypes.ORDER_CONFIRMED : EventTypes.ORDER_CANCELLED;
            Map<String, Object> finalData = new LinkedHashMap<>(event.data());
            finalData.put("status", status.name());
            outbox.add(DomainEvent.of(finalType, order.getId(), finalData));
        }
        return true;
    }
}
