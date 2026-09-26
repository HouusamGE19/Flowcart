package dev.houssam.flowcart.inventory.service;

import dev.houssam.flowcart.events.DomainEvent;
import dev.houssam.flowcart.events.EventTypes;
import dev.houssam.flowcart.inventory.domain.InventoryItem;
import dev.houssam.flowcart.inventory.domain.InventoryRepository;
import dev.houssam.flowcart.inventory.events.ProcessedEventRepository;
import dev.houssam.flowcart.inventory.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryEventHandlerTest {
    @Test
    void reservesStockAndEmitsInventoryReserved() {
        InventoryRepository inventory = mock(InventoryRepository.class);
        ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
        OutboxService outbox = mock(OutboxService.class);
        InventoryItem item = new InventoryItem("HEADSET-NC-01", 10);
        when(inventory.findById("HEADSET-NC-01")).thenReturn(Optional.of(item));

        DomainEvent event = DomainEvent.of(EventTypes.ORDER_CREATED, UUID.randomUUID(), Map.of(
                "customerEmail", "engineer@example.com",
                "totalCents", 49_800L,
                "simulatePaymentFailure", false,
                "items", List.of(Map.of("sku", "HEADSET-NC-01", "quantity", 2, "unitPriceCents", 24_900L))));

        new InventoryEventHandler(inventory, processed, outbox).handle(event);

        assertThat(item.getAvailableQuantity()).isEqualTo(8);
        assertThat(item.getReservedQuantity()).isEqualTo(2);
        ArgumentCaptor<DomainEvent> emitted = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).add(emitted.capture());
        assertThat(emitted.getValue().type()).isEqualTo(EventTypes.INVENTORY_RESERVED);
    }

    @Test
    void rejectsWhenStockIsInsufficientWithoutMutatingInventory() {
        InventoryRepository inventory = mock(InventoryRepository.class);
        ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
        OutboxService outbox = mock(OutboxService.class);
        InventoryItem item = new InventoryItem("LAPTOP-PRO-14", 1);
        when(inventory.findById("LAPTOP-PRO-14")).thenReturn(Optional.of(item));

        DomainEvent event = DomainEvent.of(EventTypes.ORDER_CREATED, UUID.randomUUID(), Map.of(
                "customerEmail", "engineer@example.com",
                "totalCents", 299_800L,
                "simulatePaymentFailure", false,
                "items", List.of(Map.of("sku", "LAPTOP-PRO-14", "quantity", 2, "unitPriceCents", 149_900L))));

        new InventoryEventHandler(inventory, processed, outbox).handle(event);

        assertThat(item.getAvailableQuantity()).isEqualTo(1);
        ArgumentCaptor<DomainEvent> emitted = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).add(emitted.capture());
        assertThat(emitted.getValue().type()).isEqualTo(EventTypes.INVENTORY_REJECTED);
    }
}
