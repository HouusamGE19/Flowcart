package dev.houssam.flowcart.catalog.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "spring.flyway.schemas=public",
        "spring.flyway.default-schema=public"
})
class CatalogPostgresTest {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("flowcart")
            .withUsername("flowcart")
            .withPassword("flowcart");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired ProductRepository repository;

    @Test
    void flywaySeedsCatalogAndJpaReadsIt() {
        assertThat(repository.findAll()).hasSize(3);
        assertThat(repository.findBySkuAndActiveTrue("LAPTOP-PRO-14")).isPresent();
    }
}
