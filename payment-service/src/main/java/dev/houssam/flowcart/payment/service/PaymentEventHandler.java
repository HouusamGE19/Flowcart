package dev.houssam.flowcart.payment.service;

import dev.houssam.flowcart.events.DomainEvent;
import dev.houssam.flowcart.events.EventData;
import dev.houssam.flowcart.events.EventTypes;
import dev.houssam.flowcart.payment.domain.Payment;
import dev.houssam.flowcart.payment.domain.PaymentRepository;
import dev.houssam.flowcart.payment.domain.PaymentStatus;
import dev.houssam.flowcart.payment.events.ProcessedEvent;
import dev.houssam.flowcart.payment.events.ProcessedEventRepository;
import dev.houssam.flowcart.payment.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentEventHandler {
    private final PaymentRepository payments; private final ProcessedEventRepository processed; private final OutboxService outbox;
    public PaymentEventHandler(PaymentRepository payments,ProcessedEventRepository processed,OutboxService outbox){this.payments=payments;this.processed=processed;this.outbox=outbox;}

    @Transactional
    public void handle(DomainEvent event){
        if(!EventTypes.INVENTORY_RESERVED.equals(event.type()) || processed.existsById(event.eventId())) return;
        if(payments.findByOrderId(event.aggregateId()).isPresent()){ processed.save(new ProcessedEvent(event.eventId())); return; }

        long amount=EventData.longValue(event.data(),"totalCents");
        boolean fail=EventData.bool(event.data(),"simulatePaymentFailure");
        UUID paymentId=UUID.randomUUID();
        PaymentStatus status=fail?PaymentStatus.FAILED:PaymentStatus.COMPLETED;
        String providerReference="SIM-"+paymentId.toString().substring(0,8).toUpperCase();
        payments.save(new Payment(paymentId,event.aggregateId(),amount,status,providerReference));

        Map<String,Object> data=new LinkedHashMap<>(event.data());
        data.put("paymentId",paymentId.toString()); data.put("providerReference",providerReference);
        if(fail) data.put("reason","Simulated payment provider decline");
        outbox.add(DomainEvent.of(fail?EventTypes.PAYMENT_FAILED:EventTypes.PAYMENT_COMPLETED,event.aggregateId(),data));
        processed.save(new ProcessedEvent(event.eventId()));
    }
}
