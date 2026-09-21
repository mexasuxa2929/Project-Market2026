package mexa.club.productservice.service;

import mexa.club.productservice.dto.ProductRequest;
import mexa.club.productservice.dto.ProductResponse;
import mexa.club.productservice.entity.Product;
import mexa.club.productservice.exception.ResourceNotFoundException;
import mexa.club.productservice.repository.ProductRepository;
import mexa.club.productservice.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private ProductService productService;

    private ProductRequest request;

    @BeforeEach
    void setUp() {
        request = new ProductRequest();
        request.setName("Keyboard");
        request.setBarcode("KB-001");
        request.setUnit("pcs");
        request.setLeadTimeDays(2);
        request.setMinStock(5);
        request.setWeight(1.1);
        request.setLength(2.0);
        request.setWidth(3.0);
        request.setHeight(4.0);
        request.setActive(true);
    }

    @Test
    void createProduct_savesAndReturnsResponse() {
        Product saved = productEntity();
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(storageService.toPublicUrl(any())).thenAnswer(inv -> "/api/files/" + inv.getArgument(0, String.class));

        ProductResponse response = productService.createProduct(request);

        assertEquals(saved.getId(), response.getId());
        assertEquals("Keyboard", response.getName());
    }

    @Test
    void getProductById_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(id, null));
    }

    @Test
    void findProducts_returnsMappedPage() {
        Product p = productEntity();
        when(productRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(p)));
        when(storageService.toPublicUrl(any())).thenAnswer(inv -> "/api/files/" + inv.getArgument(0, String.class));

        var page = productService.findProducts(PageRequest.of(0, 10), null, "key", null, null, null, null, null, null, null, null, null);

        assertEquals(1, page.getTotalElements());
        assertEquals("Keyboard", page.getContent().get(0).getName());
    }

    @Test
    void deleteProduct_whenExists_deletesEntity() throws IOException {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(true);
        org.mockito.Mockito.doNothing().when(productRepository).deleteById(id);

        productService.deleteProduct(id);
        org.mockito.Mockito.verify(productRepository).deleteById(id);
    }

    @Test
    void setProductActive_updatesFlag() {
        Product product = productEntity();
        product.setActive(true);
        UUID id = product.getId();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.toPublicUrl(any())).thenAnswer(inv -> "/api/files/" + inv.getArgument(0, String.class));

        ProductResponse updated = productService.setProductActive(id, false);

        assertFalse(updated.isActive());
    }

    private static Product productEntity() {
        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setName("Keyboard");
        p.setBarcode("KB-001");
        p.setUnit("pcs");
        p.setLeadTimeDays(2);
        p.setMinStock(5);
        p.setWeight(1.1);
        p.setLength(2.0);
        p.setWidth(3.0);
        p.setHeight(4.0);
        p.setPackageType("box");
        p.setFragile(false);
        p.setActive(true);
        p.setImagePaths(List.of("images/products/" + p.getId() + "/img.jpg"));
        return p;
    }
}
