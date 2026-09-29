package dev.houssam.flowcart.catalog.product;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return repository.findByActiveTrueOrderByNameAsc().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ProductResponse> bySku(@PathVariable String sku) {
        return repository.findBySkuAndActiveTrue(sku)
                .map(ProductResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
