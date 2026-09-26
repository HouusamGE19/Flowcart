package dev.houssam.flowcart.notification.events;

import dev.houssam.flowcart.events.DomainEvent;
import dev.houssam.flowcart.events.EventData;
import dev.houssam.flowcart.events.EventTypes;
import dev.houssam.flowcart.notification.domain.Notification;
import dev.houssam.flowcart.notification.domain.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class NotificationEventsConsumer {
    private static final Logger log= LoggerFactory.getLogger(NotificationEventsConsumer.class);
    private final NotificationRepository repository;
    public NotificationEventsConsumer(NotificationRepository repository){this.repository=repository;}

    @KafkaListener(topics="${flowcart.kafka.events-topic}",groupId="notification-service")
    @Transactional
    public void onEvent(DomainEvent event){
        if(!EventTypes.ORDER_CONFIRMED.equals(event.type()) && !EventTypes.ORDER_CANCELLED.equals(event.type())) return;
        if(repository.existsBySourceEventId(event.eventId())) return;
        String destination=EventData.string(event.data(),"customerEmail");
        if(destination==null || destination.isBlank()) destination="unknown@example.invalid";
        String template=EventTypes.ORDER_CONFIRMED.equals(event.type())?"ORDER_CONFIRMED":"ORDER_CANCELLED";
        Notification n=new Notification(UUID.randomUUID(),event.eventId(),event.aggregateId(),"EMAIL",destination,template,"SIMULATED_SENT");
        repository.save(n);
        log.info("notification_sent orderId={} destination={} template={}",event.aggregateId(),destination,template);
    }
}
