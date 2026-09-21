package mexa.club.productservice.service;

import feign.FeignException;
import mexa.club.productservice.client.ReferenceDataClient;
import mexa.club.productservice.entity.Brand;
import mexa.club.productservice.entity.Category;
import mexa.club.productservice.repository.BrandRepository;
import mexa.club.productservice.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ReferenceDataService {

    private final ReferenceDataClient referenceDataClient;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public ReferenceDataService(ReferenceDataClient referenceDataClient,
                                BrandRepository brandRepository,
                                CategoryRepository categoryRepository) {
        this.referenceDataClient = referenceDataClient;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    // ── Local DB resolution (brand & category now live in product-service DB) ──

    public String resolveCategoryName(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .map(Category::getName)
                .orElse(null);
    }

    public String resolveBrandName(UUID brandId) {
        if (brandId == null) return null;
        return brandRepository.findById(brandId)
                .map(Brand::getName)
                .orElse(null);
    }

    public String resolveManufacturerName(UUID manufacturerId) {
        return resolveSafe(() -> referenceDataClient.getManufacturer(manufacturerId));
    }

    // ── BATCH resolution (list endpointlari uchun N+1 o'rniga) ──

    /** BATCH: bir nechta kategoriya nomlarini bitta query bilan olish. */
    public Map<UUID, String> resolveCategoryNames(Collection<UUID> categoryIds) {
        Map<UUID, String> result = new HashMap<>();
        if (categoryIds == null || categoryIds.isEmpty()) {
            return result;
        }
        for (Category c : categoryRepository.findAllById(categoryIds)) {
            result.put(c.getId(), c.getName());
        }
        return result;
    }

    /** BATCH: bir nechta brand nomlarini bitta query bilan olish. */
    public Map<UUID, String> resolveBrandNames(Collection<UUID> brandIds) {
        Map<UUID, String> result = new HashMap<>();
        if (brandIds == null || brandIds.isEmpty()) {
            return result;
        }
        for (Brand b : brandRepository.findAllById(brandIds)) {
            result.put(b.getId(), b.getName());
        }
        return result;
    }

    /** BATCH: manufacturer nomlarini bitta list call bilan olish; yetishmayotganlar uchun per-id fallback. */
    public Map<UUID, String> resolveManufacturerNames(Collection<UUID> manufacturerIds) {
        Map<UUID, String> result = new HashMap<>();
        if (manufacturerIds == null || manufacturerIds.isEmpty()) {
            return result;
        }
        Set<UUID> missing = new HashSet<>(manufacturerIds);
        try {
            for (Map<String, Object> item : extractList(referenceDataClient.listManufacturers(null))) {
                Object idObj = item.get("id");
                Object nameObj = item.get("name");
                if (idObj instanceof String idStr && nameObj instanceof String nameStr) {
                    try {
                        UUID id = UUID.fromString(idStr);
                        result.put(id, nameStr);
                        missing.remove(id);
                    } catch (IllegalArgumentException ignored) {
                        // noto'g'ri UUID — o'tkazib yuboramiz
                    }
                }
            }
        } catch (FeignException ex) {
            // list call ishlamadi — quyida per-id fallback bajariladi
        }
        for (UUID id : missing) {
            String name = resolveSafe(() -> referenceDataClient.getManufacturer(id));
            if (name != null) {
                result.put(id, name);
            }
        }
        return result;
    }

    public UUID resolveOrCreateCategoryByName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return null;
        String normalized = categoryName.trim();
        // Try to find existing
        return categoryRepository.findAllByOrderByNameAsc(org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(c -> c.getName().equalsIgnoreCase(normalized))
                .findFirst()
                .map(Category::getId)
                .orElseGet(() -> {
                    // Create new
                    Category c = new Category();
                    c.setName(normalized);
                    c.setActive(true);
                    return categoryRepository.save(c).getId();
                });
    }

    public UUID resolveOrCreateBrandByName(String brandName) {
        if (brandName == null || brandName.isBlank()) return null;
        String normalized = brandName.trim();
        return brandRepository.findAllByOrderByNameAsc(org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(b -> b.getName().equalsIgnoreCase(normalized))
                .findFirst()
                .map(Brand::getId)
                .orElseGet(() -> {
                    Brand b = new Brand();
                    b.setName(normalized);
                    b.setActive(true);
                    return brandRepository.save(b).getId();
                });
    }

    public UUID resolveOrCreateManufacturerByName(String manufacturerName) {
        return resolveOrCreateByName(manufacturerName, ReferenceKind.MANUFACTURER);
    }

    private UUID resolveOrCreateByName(String value, ReferenceKind kind) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        try {
            Map<String, Object> payload = switch (kind) {
                case MANUFACTURER -> referenceDataClient.listManufacturers(normalized);
                default -> throw new IllegalArgumentException("Unsupported kind: " + kind);
            };
            UUID existing = findCategoryIdByName(payload, normalized);
            if (existing != null) {
                return existing;
            }
            Map<String, Object> created = switch (kind) {
                case MANUFACTURER -> referenceDataClient.createManufacturer(Map.of("name", normalized));
                default -> throw new IllegalArgumentException("Unsupported kind: " + kind);
            };
            UUID createdId = extractId(created);
            if (createdId != null) {
                return createdId;
            }
        } catch (FeignException ignored) {
            // fallback below
        }
        throw new NoSuchElementException("Unable to resolve/create reference: " + normalized);
    }

    public List<Map<String, Object>> listWarehousesSafe() {
        try {
            Map<String, Object> payload = referenceDataClient.listWarehouses();
            return extractList(payload);
        } catch (FeignException ex) {
            return List.of();
        }
    }

    public Optional<WarehouseStockSnapshot> fetchWarehouseStockLine(UUID warehouseId, UUID productId) {
        try {
            Map<String, Object> payload = referenceDataClient.getWarehouseStockLine(warehouseId, productId);
            Map<String, Object> data = extractDataMap(payload);
            if (data == null || data.isEmpty()) {
                return Optional.empty();
            }
            BigDecimal quantity = toBigDecimal(data.get("quantity"));
            BigDecimal reserved = toBigDecimal(data.get("reservedQuantity"));
            return Optional.of(new WarehouseStockSnapshot(
                    warehouseId,
                    quantity,
                    reserved,
                    quantity.subtract(reserved)
            ));
        } catch (FeignException ex) {
            return Optional.empty();
        }
    }

    private static String resolveSafe(ReferenceCall call) {
        try {
            Map<String, Object> payload = call.execute();
            return extractName(payload);
        } catch (FeignException | IllegalStateException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static UUID findCategoryIdByName(Map<String, Object> payload, String categoryName) {
        for (Map<String, Object> item : extractList(payload)) {
            Object name = item.get("name");
            if (name instanceof String s && s.equalsIgnoreCase(categoryName)) {
                Object id = item.get("id");
                if (id instanceof String idStr) {
                    try {
                        return UUID.fromString(idStr);
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractList(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        Object direct = payload.get("data");
        if (direct instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }
        if (direct instanceof Map<?, ?> map) {
            Object content = ((Map<String, Object>) map).get("content");
            if (content instanceof List<?> list) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof Map<?, ?> row) {
                        result.add((Map<String, Object>) row);
                    }
                }
                return result;
            }
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractDataMap(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        Object data = payload.get("data");
        if (data instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private static UUID extractId(Map<String, Object> payload) {
        Map<String, Object> data = extractDataMap(payload);
        if (data == null) {
            return null;
        }
        Object id = data.get("id");
        if (id instanceof String s) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Number n) {
            return new BigDecimal(String.valueOf(n));
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractName(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        Object directName = payload.get("name");
        if (directName instanceof String s && !s.isBlank()) {
            return s;
        }
        Object data = payload.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object nestedName = ((Map<String, Object>) dataMap).get("name");
            if (nestedName instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface ReferenceCall {
        Map<String, Object> execute();
    }

    public record WarehouseStockSnapshot(
            UUID warehouseId,
            BigDecimal quantity,
            BigDecimal reservedQuantity,
            BigDecimal availableQuantity
    ) {}

    private enum ReferenceKind {
        MANUFACTURER
    }
}
