package dev.houssam.flowcart.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="notifications")
public class Notification {
    @Id private UUID id;
    @Column(nullable=false, unique=true) private UUID sourceEventId;
    @Column(nullable=false) private UUID orderId;
    @Column(nullable=false,length=30) private String channel;
    @Column(nullable=false,length=200) private String destination;
    @Column(nullable=false,length=80) private String template;
    @Column(nullable=false,length=30) private String status;
    @Column(nullable=false) private Instant createdAt;

    protected Notification(){}
    public Notification(UUID id,UUID sourceEventId,UUID orderId,String channel,String destination,String template,String status){
        this.id=id;this.sourceEventId=sourceEventId;this.orderId=orderId;this.channel=channel;this.destination=destination;this.template=template;this.status=status;this.createdAt=Instant.now();
    }
    public UUID getId(){return id;} public UUID getSourceEventId(){return sourceEventId;} public UUID getOrderId(){return orderId;}
    public String getChannel(){return channel;} public String getDestination(){return destination;} public String getTemplate(){return template;}
    public String getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
}
