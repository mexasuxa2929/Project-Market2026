package mexa.club.productservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "referenceDataClient", url = "${app.reference-data.url}")
public interface ReferenceDataClient {

    @GetMapping("${app.reference-data.category-path:/api/categories/{id}}")
    Map<String, Object> getCategory(@PathVariable("id") UUID id);

    @GetMapping("${app.reference-data.brand-path:/api/brands/{id}}")
    Map<String, Object> getBrand(@PathVariable("id") UUID id);

    @GetMapping("${app.reference-data.manufacturer-path:/api/manufacturers/{id}}")
    Map<String, Object> getManufacturer(@PathVariable("id") UUID id);

    @GetMapping("/api/categories")
    Map<String, Object> listCategories(@RequestParam(value = "name", required = false) String name);

    @PostMapping("/api/categories")
    Map<String, Object> createCategory(@RequestBody Map<String, Object> body);

    @GetMapping("/api/brands")
    Map<String, Object> listBrands(@RequestParam(value = "name", required = false) String name);

    @PostMapping("/api/brands")
    Map<String, Object> createBrand(@RequestBody Map<String, Object> body);

    @GetMapping("/api/manufacturers")
    Map<String, Object> listManufacturers(@RequestParam(value = "name", required = false) String name);

    @PostMapping("/api/manufacturers")
    Map<String, Object> createManufacturer(@RequestBody Map<String, Object> body);

    @GetMapping("/api/warehouses")
    Map<String, Object> listWarehouses();

    @GetMapping("/api/warehouses/{warehouseId}/stock/lines/{productId}")
    Map<String, Object> getWarehouseStockLine(@PathVariable("warehouseId") UUID warehouseId, @PathVariable("productId") UUID productId);
}
