package dev.houssam.flowcart.order.sse;

import dev.houssam.flowcart.events.DomainEvent;
import dev.houssam.flowcart.order.api.OrderResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class OrderEventStream {
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(UUID orderId, OrderResponse snapshot) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(orderId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(orderId, emitter));
        emitter.onTimeout(() -> remove(orderId, emitter));
        emitter.onError(error -> remove(orderId, emitter));
        try {
            emitter.send(SseEmitter.event().name("snapshot").data(snapshot));
        } catch (IOException e) {
            remove(orderId, emitter);
        }
        return emitter;
    }

    public void emit(DomainEvent event) {
        List<SseEmitter> subscribers = emitters.getOrDefault(event.aggregateId(), new CopyOnWriteArrayList<>());
        for (SseEmitter emitter : subscribers) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.eventId().toString())
                        .name(event.type())
                        .data(event));
            } catch (IOException e) {
                remove(event.aggregateId(), emitter);
            }
        }
    }

    private void remove(UUID orderId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> orderEmitters = emitters.get(orderId);
        if (orderEmitters != null) {
            orderEmitters.remove(emitter);
            if (orderEmitters.isEmpty()) {
                emitters.remove(orderId);
            }
        }
    }
}
