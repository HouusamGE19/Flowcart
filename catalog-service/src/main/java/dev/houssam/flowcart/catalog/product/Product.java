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

    @Column(length = 80)
    private String brand;

    @Column(name = "style_code", length = 80)
    private String styleCode;

    @Column(length = 180)
    private String colorway;

    @Column(length = 700)
    private String description;

    @Column(name = "image_url", length = 1200)
    private String imageUrl;

    protected Product() {
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public long getPriceCents() { return priceCents; }
    public boolean isActive() { return active; }
    public String getBrand() { return brand; }
    public String getStyleCode() { return styleCode; }
    public String getColorway() { return colorway; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
}
