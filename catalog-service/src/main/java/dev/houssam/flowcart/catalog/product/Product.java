package dev.houssam.flowcart.catalog.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private long priceCents;

    @Column(nullable = false)
    private boolean active;

    protected Product() {
    }

    public Product(UUID id, String sku, String name, long priceCents, boolean active) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.priceCents = priceCents;
        this.active = active;
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public long getPriceCents() { return priceCents; }
    public boolean isActive() { return active; }
}
