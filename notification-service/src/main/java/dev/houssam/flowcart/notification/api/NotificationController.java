package dev.houssam.flowcart.notification.api;

import dev.houssam.flowcart.notification.domain.Notification;
import dev.houssam.flowcart.notification.domain.NotificationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository repository; public NotificationController(NotificationRepository repository){this.repository=repository;}
    @GetMapping public List<Response> recent(){return repository.findTop100ByOrderByCreatedAtDesc().stream().map(Response::from).toList();}
    public record Response(UUID id,UUID orderId,String channel,String destination,String template,String status,Instant createdAt){
        static Response from(Notification n){return new Response(n.getId(),n.getOrderId(),n.getChannel(),n.getDestination(),n.getTemplate(),n.getStatus(),n.getCreatedAt());}
    }
}
