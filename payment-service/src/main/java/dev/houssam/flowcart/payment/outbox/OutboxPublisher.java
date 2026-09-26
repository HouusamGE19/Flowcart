package dev.houssam.flowcart.payment.outbox;

import tools.jackson.databind.ObjectMapper;
import dev.houssam.flowcart.events.DomainEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {
    private final OutboxRepository repository; private final ObjectMapper mapper; private final KafkaTemplate<String,DomainEvent> kafka; private final String topic;
    public OutboxPublisher(OutboxRepository repository,ObjectMapper mapper,KafkaTemplate<String,DomainEvent> kafka,@Value("${flowcart.kafka.events-topic}")String topic){this.repository=repository;this.mapper=mapper;this.kafka=kafka;this.topic=topic;}
    @Scheduled(fixedDelayString="${flowcart.outbox.publish-delay-ms:500}") @Transactional
    public void publishPending(){
        for(OutboxEvent row:repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()){
            try{DomainEvent event=mapper.readValue(row.getPayload(),DomainEvent.class);kafka.send(topic,event.aggregateId().toString(),event).get(5, TimeUnit.SECONDS);row.markPublished();}
            catch(Exception e){break;}
        }
    }
}
