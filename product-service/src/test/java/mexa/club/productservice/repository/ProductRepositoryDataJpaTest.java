package mexa.club.productservice.repository;

import mexa.club.productservice.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:productdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class ProductRepositoryDataJpaTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findByBarcode_returnsSavedEntity() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Keyboard");
        product.setBarcode("KB-100");
        product.setUnit("pcs");
        product.setActive(true);
        productRepository.save(product);

        Optional<Product> found = productRepository.findByBarcode("KB-100");
        assertTrue(found.isPresent());
    }

    @Test
    void productSpecification_categoryAndBrandFilters_workCorrectly() {
        UUID categoryA = UUID.randomUUID();
        UUID categoryB = UUID.randomUUID();
        UUID brandA = UUID.randomUUID();
        UUID brandB = UUID.randomUUID();

        Product p1 = new Product();
        p1.setId(UUID.randomUUID());
        p1.setName("Keyboard A");
        p1.setBarcode("KB-A");
        p1.setCategoryId(categoryA);
        p1.setBrandId(brandA);
        p1.setUnit("pcs");
        p1.setActive(true);

        Product p2 = new Product();
        p2.setId(UUID.randomUUID());
        p2.setName("Keyboard B");
        p2.setBarcode("KB-B");
        p2.setCategoryId(categoryB);
        p2.setBrandId(brandB);
        p2.setUnit("pcs");
        p2.setActive(true);

        productRepository.save(p1);
        productRepository.save(p2);

        var result = productRepository.findAll(ProductSpecification.combined(null, null, categoryA, brandA, null, null, null, null, null));
        assertEquals(1, result.size());
        assertEquals("KB-A", result.get(0).getBarcode());
    }
}
