package dev.houssam.flowcart.order.events;

import dev.houssam.flowcart.events.DomainEvent;
import dev.houssam.flowcart.order.service.OrderEventHandler;
import dev.houssam.flowcart.order.sse.OrderEventStream;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventsConsumer {
    private final OrderEventHandler handler;
    private final OrderEventStream stream;

    public OrderEventsConsumer(OrderEventHandler handler, OrderEventStream stream) {
        this.handler = handler;
        this.stream = stream;
    }

    @KafkaListener(topics = "${flowcart.kafka.events-topic}", groupId = "order-service")
    public void onEvent(DomainEvent event) {
        if (handler.handle(event)) {
            stream.emit(event);
        }
    }
}
