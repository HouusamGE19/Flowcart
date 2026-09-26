package dev.houssam.flowcart.payment.api;

import dev.houssam.flowcart.payment.domain.Payment;
import dev.houssam.flowcart.payment.domain.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant; import java.util.UUID;

@RestController @RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentRepository repository; public PaymentController(PaymentRepository repository){this.repository=repository;}
    @GetMapping("/order/{orderId}") public ResponseEntity<PaymentResponse> byOrder(@PathVariable UUID orderId){return repository.findByOrderId(orderId).map(PaymentResponse::from).map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());}
    public record PaymentResponse(UUID id,UUID orderId,long amountCents,String status,String providerReference,Instant createdAt){static PaymentResponse from(Payment p){return new PaymentResponse(p.getId(),p.getOrderId(),p.getAmountCents(),p.getStatus().name(),p.getProviderReference(),p.getCreatedAt());}}
}
