package dev.houssam.flowcart.payment.service;

import dev.houssam.flowcart.events.DomainEvent;
import dev.houssam.flowcart.events.EventTypes;
import dev.houssam.flowcart.payment.domain.Payment;
import dev.houssam.flowcart.payment.domain.PaymentRepository;
import dev.houssam.flowcart.payment.events.ProcessedEventRepository;
import dev.houssam.flowcart.payment.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentEventHandlerTest {
    @Test
    void forcedFailurePersistsFailedPaymentAndEmitsCompensatingSignal() {
        PaymentRepository payments = mock(PaymentRepository.class);
        ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
        OutboxService outbox = mock(OutboxService.class);
        UUID orderId = UUID.randomUUID();
        when(payments.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DomainEvent event = DomainEvent.of(EventTypes.INVENTORY_RESERVED, orderId, Map.of(
                "customerEmail", "engineer@example.com",
                "totalCents", 12_900L,
                "simulatePaymentFailure", true,
                "items", List.of(Map.of("sku", "KEYBOARD-MECH-01", "quantity", 1, "unitPriceCents", 12_900L))));

        new PaymentEventHandler(payments, processed, outbox).handle(event);

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(payment.capture());
        assertThat(payment.getValue().getStatus().name()).isEqualTo("FAILED");

        ArgumentCaptor<DomainEvent> emitted = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).add(emitted.capture());
        assertThat(emitted.getValue().type()).isEqualTo(EventTypes.PAYMENT_FAILED);
        assertThat(emitted.getValue().data()).containsKey("providerReference");
    }
}
