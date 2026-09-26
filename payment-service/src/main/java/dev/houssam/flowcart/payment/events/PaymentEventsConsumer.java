package dev.houssam.flowcart.payment.events;

import dev.houssam.flowcart.events.DomainEvent;
import dev.houssam.flowcart.payment.service.PaymentEventHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventsConsumer {
    private final PaymentEventHandler handler; public PaymentEventsConsumer(PaymentEventHandler handler){this.handler=handler;}
    @KafkaListener(topics="${flowcart.kafka.events-topic}",groupId="payment-service") public void onEvent(DomainEvent event){handler.handle(event);}
}
