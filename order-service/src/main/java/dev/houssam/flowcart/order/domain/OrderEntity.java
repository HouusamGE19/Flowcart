package dev.houssam.flowcart.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OrderStatus status;

    @Column(nullable = false)
    private long totalCents;

    @Column(nullable = false)
    private boolean simulatePaymentFailure;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    protected OrderEntity() {
    }

    public OrderEntity(UUID id, String customerEmail, long totalCents, boolean simulatePaymentFailure) {
        this.id = id;
        this.customerEmail = customerEmail;
        this.status = OrderStatus.CREATED;
        this.totalCents = totalCents;
        this.simulatePaymentFailure = simulatePaymentFailure;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void addItem(String sku, int quantity, long unitPriceCents) {
        items.add(new OrderItemEntity(UUID.randomUUID(), this, sku, quantity, unitPriceCents));
    }

    public void transitionTo(OrderStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCustomerEmail() { return customerEmail; }
    public OrderStatus getStatus() { return status; }
    public long getTotalCents() { return totalCents; }
    public boolean isSimulatePaymentFailure() { return simulatePaymentFailure; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<OrderItemEntity> getItems() { return List.copyOf(items); }
}
