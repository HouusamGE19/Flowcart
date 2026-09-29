package dev.houssam.flowcart.order.catalog;

import java.util.UUID;

public record CatalogProduct(
        UUID id,
        String sku,
        String name,
        long priceCents,
        boolean active,
        String brand,
        String styleCode,
        String colorway,
        String description,
        String imageUrl
) {
}
