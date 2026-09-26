package dev.houssam.flowcart.order.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    @Override
    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findById(UUID id);

    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findTop50ByOrderByCreatedAtDesc();
}
