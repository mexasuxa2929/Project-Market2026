package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.dto.StockBatchByWarehouseResponse;
import mexa.club.warehouseproject.dto.StockBatchResponse;
import mexa.club.warehouseproject.entity.WarehouseStock;
import mexa.club.warehouseproject.repository.WarehouseStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StockBatchService {

    private final WarehouseStockRepository warehouseStockRepository;

    public StockBatchService(WarehouseStockRepository warehouseStockRepository) {
        this.warehouseStockRepository = warehouseStockRepository;
    }

    /**
     * Returns total stock (SUM across all warehouses) for each requested productId.
     * Products with no stock records get totalStock=0.
     */
    @Transactional(readOnly = true)
    public List<StockBatchResponse> getTotalStockBatch(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<WarehouseStock> stockLines = warehouseStockRepository.findAllByProductIdIn(productIds);

        // Sum quantities per productId
        Map<UUID, Integer> totals = new LinkedHashMap<>();
        for (UUID id : productIds) {
            totals.put(id, 0); // ensure all requested products are present in response
        }
        for (WarehouseStock s : stockLines) {
            BigDecimal qty = s.getQuantity() != null ? s.getQuantity() : BigDecimal.ZERO;
            BigDecimal reserved = s.getReservedQuantity() != null ? s.getReservedQuantity() : BigDecimal.ZERO;
            int available = qty.subtract(reserved).max(BigDecimal.ZERO).intValue();
            totals.merge(s.getProductId(), available, Integer::sum);
        }

        return totals.entrySet().stream()
                .map(e -> StockBatchResponse.builder()
                        .productId(e.getKey())
                        .totalStock(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Returns per-warehouse stock breakdown for each requested productId.
     */
    @Transactional(readOnly = true)
    public List<StockBatchByWarehouseResponse> getStockByWarehouseBatch(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<WarehouseStock> stockLines = warehouseStockRepository.findAllByProductIdIn(productIds);

        Map<UUID, List<StockBatchByWarehouseResponse.WarehouseBreakdown>> grouped = new LinkedHashMap<>();
        for (UUID id : productIds) {
            grouped.put(id, new ArrayList<>());
        }
        for (WarehouseStock s : stockLines) {
            BigDecimal qty = s.getQuantity() != null ? s.getQuantity() : BigDecimal.ZERO;
            BigDecimal reserved = s.getReservedQuantity() != null ? s.getReservedQuantity() : BigDecimal.ZERO;
            BigDecimal available = qty.subtract(reserved).max(BigDecimal.ZERO);
            String warehouseName = s.getWarehouse() != null ? s.getWarehouse().getName() : null;
            UUID warehouseId = s.getWarehouse() != null ? s.getWarehouse().getId() : null;
            grouped.computeIfAbsent(s.getProductId(), k -> new ArrayList<>())
                    .add(StockBatchByWarehouseResponse.WarehouseBreakdown.builder()
                            .warehouseId(warehouseId)
                            .warehouseName(warehouseName)
                            .quantity(qty)
                            .reservedQuantity(reserved)
                            .availableQuantity(available)
                            .build());
        }

        return grouped.entrySet().stream()
                .map(e -> StockBatchByWarehouseResponse.builder()
                        .productId(e.getKey())
                        .warehouses(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }
}
