package dev.houssam.flowcart.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private UUID orderId;
    @Column(nullable = false) private long amountCents;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private PaymentStatus status;
    @Column(nullable = false, length = 200) private String providerReference;
    @Column(nullable = false) private Instant createdAt;

    protected Payment() {}
    public Payment(UUID id, UUID orderId, long amountCents, PaymentStatus status, String providerReference) {
        this.id=id; this.orderId=orderId; this.amountCents=amountCents; this.status=status; this.providerReference=providerReference; this.createdAt=Instant.now();
    }
    public UUID getId(){return id;} public UUID getOrderId(){return orderId;} public long getAmountCents(){return amountCents;}
    public PaymentStatus getStatus(){return status;} public String getProviderReference(){return providerReference;} public Instant getCreatedAt(){return createdAt;}
}
