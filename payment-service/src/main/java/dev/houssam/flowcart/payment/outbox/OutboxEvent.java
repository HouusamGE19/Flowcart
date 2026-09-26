package dev.houssam.flowcart.payment.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="outbox_events")
public class OutboxEvent {
    @Id private UUID id;
    @Column(nullable=false) private UUID aggregateId;
    @Column(nullable=false,length=100) private String eventType;
    @Column(nullable=false,columnDefinition="TEXT") private String payload;
    @Column(nullable=false) private Instant createdAt;
    private Instant publishedAt;
    protected OutboxEvent(){}
    public OutboxEvent(UUID id,UUID aggregateId,String eventType,String payload){this.id=id;this.aggregateId=aggregateId;this.eventType=eventType;this.payload=payload;this.createdAt=Instant.now();}
    public void markPublished(){this.publishedAt=Instant.now();} public String getPayload(){return payload;}
}
