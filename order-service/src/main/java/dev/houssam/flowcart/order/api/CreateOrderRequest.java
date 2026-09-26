package dev.houssam.flowcart.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateOrderRequest(
        @NotNull @Email String customerEmail,
        @NotEmpty List<@Valid Item> items,
        boolean simulatePaymentFailure
) {
    public record Item(@NotNull String sku, @Positive int quantity) {
    }
}
