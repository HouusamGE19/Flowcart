package dev.houssam.flowcart.order.outbox;

import tools.jackson.databind.ObjectMapper;
import dev.houssam.flowcart.events.DomainEvent;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void add(DomainEvent event) {
        try {
            repository.save(new OutboxEventEntity(
                    event.eventId(), event.aggregateId(), event.type(), objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize outbox event", e);
        }
    }
}
