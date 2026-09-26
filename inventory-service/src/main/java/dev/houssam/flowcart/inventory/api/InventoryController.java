package dev.houssam.flowcart.inventory.api;

import dev.houssam.flowcart.inventory.domain.InventoryItem;
import dev.houssam.flowcart.inventory.domain.InventoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryRepository repository;
    public InventoryController(InventoryRepository repository) { this.repository = repository; }

    @GetMapping
    public List<InventoryResponse> list() {
        return repository.findAll().stream().map(InventoryResponse::from).toList();
    }

    @PostMapping("/{sku}/restock")
    @Transactional
    public ResponseEntity<InventoryResponse> restock(@PathVariable String sku, @RequestParam int quantity) {
        return repository.findById(sku).map(item -> {
            item.restock(quantity);
            return ResponseEntity.ok(InventoryResponse.from(item));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record InventoryResponse(String sku, int availableQuantity, int reservedQuantity, long version) {
        static InventoryResponse from(InventoryItem item) {
            return new InventoryResponse(item.getSku(), item.getAvailableQuantity(), item.getReservedQuantity(), item.getVersion());
        }
    }
}
