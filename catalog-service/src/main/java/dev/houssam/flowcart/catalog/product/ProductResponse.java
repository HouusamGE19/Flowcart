package dev.houssam.flowcart.catalog.product;

import java.util.UUID;

public record ProductResponse(UUID id, String sku, String name, long priceCents, boolean active) {
    static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getPriceCents(), product.isActive());
    }
}
