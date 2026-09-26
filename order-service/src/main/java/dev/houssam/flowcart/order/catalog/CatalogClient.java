package dev.houssam.flowcart.order.catalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CatalogClient {
    private final RestClient client;

    public CatalogClient(RestClient.Builder builder, @Value("${flowcart.catalog.base-url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public CatalogProduct requireProduct(String sku) {
        CatalogProduct product = client.get()
                .uri("/api/products/{sku}", sku)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ProductNotFoundException(sku);
                })
                .body(CatalogProduct.class);
        if (product == null || !product.active()) {
            throw new ProductNotFoundException(sku);
        }
        return product;
    }

    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String sku) {
            super("Unknown or inactive SKU: " + sku);
        }
    }
}
