package dev.houssam.flowcart.inventory.events;

import dev.houssam.flowcart.events.DomainEvent;
import dev.houssam.flowcart.inventory.service.InventoryEventHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventsConsumer {
    private final InventoryEventHandler handler;
    public InventoryEventsConsumer(InventoryEventHandler handler) { this.handler = handler; }

    @KafkaListener(topics = "${flowcart.kafka.events-topic}", groupId = "inventory-service")
    public void onEvent(DomainEvent event) { handler.handle(event); }
}
