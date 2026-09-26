package dev.houssam.flowcart.inventory.outbox;

import tools.jackson.databind.ObjectMapper;
import dev.houssam.flowcart.events.DomainEvent;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {
    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository repository, ObjectMapper objectMapper) {
        this.repository = repository; this.objectMapper = objectMapper;
    }

    public void add(DomainEvent event) {
        try {
            repository.save(new OutboxEvent(event.eventId(), event.aggregateId(), event.type(), objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize inventory outbox event", e);
        }
    }
}
