package dev.houssam.flowcart.order.outbox;

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
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final String topic;

    public OutboxPublisher(OutboxEventRepository repository,
                           ObjectMapper objectMapper,
                           KafkaTemplate<String, DomainEvent> kafkaTemplate,
                           @Value("${flowcart.kafka.events-topic}") String topic) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${flowcart.outbox.publish-delay-ms:500}")
    @Transactional
    public void publishPending() {
        for (OutboxEventEntity row : repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                DomainEvent event = objectMapper.readValue(row.getPayload(), DomainEvent.class);
                kafkaTemplate.send(topic, event.aggregateId().toString(), event).get(5, TimeUnit.SECONDS);
                row.markPublished();
            } catch (Exception e) {
                // Leave the row unpublished. The next scheduler pass retries it.
                break;
            }
        }
    }
}
