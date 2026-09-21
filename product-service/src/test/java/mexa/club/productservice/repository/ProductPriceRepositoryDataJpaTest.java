package mexa.club.productservice.repository;

import mexa.club.productservice.entity.ProductPrice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:productpricedb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class ProductPriceRepositoryDataJpaTest {

    @Autowired
    private ProductPriceRepository productPriceRepository;

    @Test
    void findByProductIdOrderByEffectiveDateDesc_returnsSortedPrices() {
        UUID productId = UUID.randomUUID();
        LocalDateTime base = LocalDateTime.now().minusHours(1);
        ProductPrice older = new ProductPrice(UUID.randomUUID(), productId, BigDecimal.ONE, BigDecimal.TEN,
                base, null);
        ProductPrice newer = new ProductPrice(UUID.randomUUID(), productId, BigDecimal.valueOf(2), BigDecimal.valueOf(12),
                base.plusMinutes(1), null);

        productPriceRepository.save(older);
        productPriceRepository.save(newer);

        List<ProductPrice> prices = productPriceRepository.findByProductIdOrderByEffectiveDateDesc(productId);
        assertEquals(2, prices.size());
        assertEquals(base.plusMinutes(1), prices.get(0).getEffectiveDate());
    }
}
