package dev.houssam.flowcart.order.service;

import dev.houssam.flowcart.events.DomainEvent;
import dev.houssam.flowcart.events.EventTypes;
import dev.houssam.flowcart.order.api.CreateOrderRequest;
import dev.houssam.flowcart.order.catalog.CatalogClient;
import dev.houssam.flowcart.order.catalog.CatalogProduct;
import dev.houssam.flowcart.order.domain.OrderEntity;
import dev.houssam.flowcart.order.domain.OrderRepository;
import dev.houssam.flowcart.order.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderApplicationServiceTest {
    @Test
    void pricesOrderFromCatalogAndWritesOrderCreatedToOutbox() {
        OrderRepository orders = mock(OrderRepository.class);
        CatalogClient catalog = mock(CatalogClient.class);
        OutboxService outbox = mock(OutboxService.class);
        when(catalog.requireProduct("KEYBOARD-MECH-01"))
                .thenReturn(new CatalogProduct(UUID.randomUUID(), "KEYBOARD-MECH-01", "Mechanical Keyboard", 12_900, true));
        when(orders.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new OrderApplicationService(orders, catalog, outbox);
        var response = service.create(new CreateOrderRequest(
                "engineer@example.com",
                List.of(new CreateOrderRequest.Item("KEYBOARD-MECH-01", 2)),
                false));

        assertThat(response.totalCents()).isEqualTo(25_800);
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.sku()).isEqualTo("KEYBOARD-MECH-01");
            assertThat(item.quantity()).isEqualTo(2);
        });

        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).add(event.capture());
        assertThat(event.getValue().type()).isEqualTo(EventTypes.ORDER_CREATED);
        assertThat(event.getValue().aggregateId()).isEqualTo(response.id());
        assertThat(event.getValue().data()).containsEntry("totalCents", 25_800L);
    }
}
