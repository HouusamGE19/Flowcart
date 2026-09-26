package dev.houssam.flowcart.events;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        String type,
        UUID aggregateId,
        Instant occurredAt,
        Map<String, Object> data
) {
    public DomainEvent {
        data = data == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(data));
    }

    public static DomainEvent of(String type, UUID aggregateId, Map<String, Object> data) {
        return new DomainEvent(UUID.randomUUID(), type, aggregateId, Instant.now(), data);
    }
}
