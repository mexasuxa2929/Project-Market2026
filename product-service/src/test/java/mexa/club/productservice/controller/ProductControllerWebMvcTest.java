package mexa.club.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mexa.club.productservice.dto.ProductRequest;
import mexa.club.productservice.dto.ProductResponse;
import mexa.club.productservice.security.JwtRequestFilter;
import mexa.club.productservice.security.JwtUtil;
import mexa.club.productservice.security.SwaggerAccessFilter;
import mexa.club.productservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private JwtRequestFilter jwtRequestFilter;
    @MockBean
    private SwaggerAccessFilter swaggerAccessFilter;

    @Test
    void listProducts_returnsPagedResponse() throws Exception {
        ProductResponse response = sampleResponse();
when(productService.findProducts(any(PageRequest.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Keyboard"));
    }

    @Test
    void createProduct_returnsCreated() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("Keyboard");
        request.setBarcode("KB-1");
        request.setUnit("pcs");
        request.setLeadTimeDays(1);
        request.setMinStock(0);
        request.setWeight(0.1);
        request.setLength(1.0);
        request.setWidth(1.0);
        request.setHeight(1.0);
        request.setActive(true);

        when(productService.createProduct(any(ProductRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Keyboard"));
    }

    @Test
    void patchActive_returnsUpdatedResponse() throws Exception {
        UUID id = UUID.randomUUID();
        ProductResponse inactive = sampleResponse();
        inactive.setActive(false);
        when(productService.setProductActive(eq(id), eq(false))).thenReturn(inactive);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/products/{id}/active", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    private static ProductResponse sampleResponse() {
        ProductResponse r = new ProductResponse();
        r.setId(UUID.randomUUID());
        r.setName("Keyboard");
        r.setBarcode("KB-1");
        r.setUnit("pcs");
        r.setLeadTimeDays(1);
        r.setMinStock(0);
        r.setWeight(0.1);
        r.setLength(1.0);
        r.setWidth(1.0);
        r.setHeight(1.0);
        r.setPackageType("box");
        r.setFragile(false);
        r.setActive(true);
        r.setImageUrls(List.of());
        return r;
    }
}
