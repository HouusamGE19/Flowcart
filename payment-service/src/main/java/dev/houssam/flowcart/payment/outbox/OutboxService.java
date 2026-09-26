package dev.houssam.flowcart.payment.outbox;

import tools.jackson.databind.ObjectMapper;
import dev.houssam.flowcart.events.DomainEvent;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {
    private final OutboxRepository repository; private final ObjectMapper mapper;
    public OutboxService(OutboxRepository repository,ObjectMapper mapper){this.repository=repository;this.mapper=mapper;}
    public void add(DomainEvent event){
        try{repository.save(new OutboxEvent(event.eventId(),event.aggregateId(),event.type(),mapper.writeValueAsString(event)));}
        catch(Exception e){throw new IllegalStateException("Could not serialize payment outbox event",e);}
    }
}
