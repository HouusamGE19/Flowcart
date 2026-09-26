package dev.houssam.flowcart.order.api;

import dev.houssam.flowcart.order.domain.OrderEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerEmail,
        String status,
        long totalCents,
        boolean simulatePaymentFailure,
        Instant createdAt,
        Instant updatedAt,
        List<Item> items
) {
    public static OrderResponse from(OrderEntity order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerEmail(),
                order.getStatus().name(),
                order.getTotalCents(),
                order.isSimulatePaymentFailure(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(i -> new Item(i.getSku(), i.getQuantity(), i.getUnitPriceCents())).toList()
        );
    }

    public record Item(String sku, int quantity, long unitPriceCents) {
    }
}
