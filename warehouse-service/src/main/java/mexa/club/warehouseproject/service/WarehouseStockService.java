package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.dto.CapacitySummaryResponse;
import mexa.club.warehouseproject.dto.InternalWarehouseStatsResponse;
import mexa.club.warehouseproject.dto.LowStockAlertResponse;
import mexa.club.warehouseproject.dto.SalesOutRequest;
import mexa.club.warehouseproject.dto.SalesOutResponse;
import mexa.club.warehouseproject.dto.StockAdjustRequest;
import mexa.club.warehouseproject.dto.StockMovementResponse;
import mexa.club.warehouseproject.dto.StockUpsertRequest;
import mexa.club.warehouseproject.dto.StockLotResponse;
import mexa.club.warehouseproject.dto.WarehouseStockLineResponse;
import mexa.club.warehouseproject.entity.MovementType;
import mexa.club.warehouseproject.entity.StockLot;
import mexa.club.warehouseproject.entity.Warehouse;
import mexa.club.warehouseproject.entity.WarehouseStock;
import mexa.club.warehouseproject.exception.ResourceConflictException;
import mexa.club.warehouseproject.exception.ResourceNotFoundException;
import mexa.club.warehouseproject.repository.StockMovementRepository;
import mexa.club.warehouseproject.repository.StockLotRepository;
import mexa.club.warehouseproject.repository.WarehouseRepository;
import mexa.club.warehouseproject.repository.WarehouseStockRepository;
import mexa.club.warehouseproject.security.WarehouseAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WarehouseStockService {

    public record WarehouseSummaryStats(
            int productCount,
            int lowStockCount,
            int todayIncoming,
            BigDecimal usedVolumeM3,
            int capacityPct
    ) {}

    private static final Logger log = LoggerFactory.getLogger(WarehouseStockService.class);
    private static final String UNKNOWN_PRODUCT_NAME = "Unknown";

    private final WarehouseStockRepository warehouseStockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductReferenceService productReferenceService;
    private final WarehouseAccessService warehouseAccessService;
    private final AuditLogService auditLogService;
    private final StockMovementService stockMovementService;
    private final StockMovementRepository stockMovementRepository;
    private final StockLotService stockLotService;
    private final StockLotRepository stockLotRepository;

    public WarehouseStockService(
            WarehouseStockRepository warehouseStockRepository,
            WarehouseRepository warehouseRepository,
            ProductReferenceService productReferenceService,
            WarehouseAccessService warehouseAccessService,
            AuditLogService auditLogService,
            StockMovementService stockMovementService,
            StockMovementRepository stockMovementRepository,
            StockLotService stockLotService,
            StockLotRepository stockLotRepository
    ) {
        this.warehouseStockRepository = warehouseStockRepository;
        this.warehouseRepository = warehouseRepository;
        this.productReferenceService = productReferenceService;
        this.warehouseAccessService = warehouseAccessService;
        this.auditLogService = auditLogService;
        this.stockMovementService = stockMovementService;
        this.stockMovementRepository = stockMovementRepository;
        this.stockLotService = stockLotService;
        this.stockLotRepository = stockLotRepository;
    }

    @Transactional(readOnly = true)
    public Page<WarehouseStockLineResponse> listStock(UUID warehouseId, Pageable pageable, String search) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId));
        }
        if (search != null && !search.isBlank()) {
            List<UUID> productIds = productReferenceService.searchProductIds(search);
            if (productIds.isEmpty()) {
                return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0);
            }
            return warehouseStockRepository.findByWarehouseIdAndProductIdIn(warehouseId, productIds, pageable)
                    .map(this::toResponseWithSafeProductName);
        }
        return warehouseStockRepository.findByWarehouseId(warehouseId, pageable)
                .map(this::toResponseWithSafeProductName);
    }

    @Transactional(readOnly = true)
    public WarehouseStockLineResponse getStockLineByProduct(UUID warehouseId, UUID productId) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId));
        }
        WarehouseStock line = warehouseStockRepository.findByWarehouse_IdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse stock line",
                        "warehouseId=" + warehouseId + ", productId=" + productId
                ));
        return toResponseWithSafeProductName(line);
    }

    @Transactional(readOnly = true)
    public List<LowStockAlertResponse> listLowStockAlerts(UUID warehouseId) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId));
        }
        return internalLowStockAlerts(warehouseId);
    }

    /**
     * Security context talab qilmaydigan (scheduler ichida ishlatiladi) low-stock hisoblash.
     */
    @Transactional(readOnly = true)
    public List<LowStockAlertResponse> internalLowStockAlerts(UUID warehouseId) {
        List<WarehouseStock> lines = warehouseStockRepository.findAllByWarehouse_Id(warehouseId);
        if (lines.isEmpty()) {
            return List.of();
        }

        Set<UUID> productIds = lines.stream().map(WarehouseStock::getProductId).collect(Collectors.toSet());
        Map<UUID, ProductReferenceService.ProductSnapshot> products = productReferenceService.fetchProducts(productIds);

        return lines.stream()
                .map(line -> {
                    ProductReferenceService.ProductSnapshot p = products.get(line.getProductId());
                    int minStock = line.getMinStockOverride() != null
                            ? line.getMinStockOverride()
                            : (p != null ? p.minStock() : 0);
                    BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
                    BigDecimal reserved = line.getReservedQuantity() != null ? line.getReservedQuantity() : BigDecimal.ZERO;
                    BigDecimal available = qty.subtract(reserved);
                    if (available.compareTo(BigDecimal.valueOf(minStock)) > 0) {
                        return null;
                    }
                    return LowStockAlertResponse.builder()
                            .warehouseId(warehouseId)
                            .productId(line.getProductId())
                            .productName(p != null ? p.name() : UNKNOWN_PRODUCT_NAME)
                            .minStock(minStock)
                            .minStockOverride(line.getMinStockOverride())
                            .quantity(qty)
                            .reservedQuantity(reserved)
                            .availableQuantity(available)
                            .build();
                })
                .filter(a -> a != null)
                .sorted(Comparator.comparing(LowStockAlertResponse::getAvailableQuantity)
                        .thenComparing(LowStockAlertResponse::getProductName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CapacitySummaryResponse capacitySummary(UUID warehouseId) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));

        BigDecimal capacityM3 = BigDecimal.valueOf(warehouse.getCapacity() != null ? warehouse.getCapacity() : 0);

        List<WarehouseStock> lines = warehouseStockRepository.findAllByWarehouse_Id(warehouseId);
        if (lines.isEmpty()) {
            return CapacitySummaryResponse.builder()
                    .capacityM3(capacityM3)
                    .usedVolumeM3(BigDecimal.ZERO)
                    .capacityPct(0)
                    .items(List.of())
                    .build();
        }

        Set<UUID> productIds = lines.stream().map(WarehouseStock::getProductId).collect(Collectors.toSet());
        Map<UUID, ProductReferenceService.ProductSnapshot> products = productReferenceService.fetchProducts(productIds);

        BigDecimal used = BigDecimal.ZERO;
        List<CapacitySummaryResponse.CapacityItemResponse> items = new ArrayList<>();
        for (WarehouseStock line : lines) {
            ProductReferenceService.ProductSnapshot p = products.get(line.getProductId());
            double length = p != null ? p.length() : 0.0;
            double width = p != null ? p.width() : 0.0;
            double height = p != null ? p.height() : 0.0;
            BigDecimal unitVolumeM3 = BigDecimal.valueOf(Math.max(0.0, length) * Math.max(0.0, width) * Math.max(0.0, height))
                    .divide(BigDecimal.valueOf(1_000_000), 6, java.math.RoundingMode.HALF_UP);
            BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
            BigDecimal volumeM3 = qty.multiply(unitVolumeM3).setScale(6, java.math.RoundingMode.HALF_UP);
            used = used.add(volumeM3);
            items.add(CapacitySummaryResponse.CapacityItemResponse.builder()
                    .productId(line.getProductId())
                    .productName(p != null ? p.name() : UNKNOWN_PRODUCT_NAME)
                    .quantity(qty)
                    .length(length)
                    .width(width)
                    .height(height)
                    .unitVolumeM3(unitVolumeM3)
                    .volumeM3(volumeM3)
                    .build());
        }

        int capacityPct = 0;
        if (capacityM3.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pct = used.multiply(BigDecimal.valueOf(100))
                    .divide(capacityM3, 2, java.math.RoundingMode.HALF_UP);
            capacityPct = pct.min(BigDecimal.valueOf(100)).intValue();
        }

        return CapacitySummaryResponse.builder()
                .capacityM3(capacityM3)
                .usedVolumeM3(used.setScale(6, java.math.RoundingMode.HALF_UP))
                .capacityPct(capacityPct)
                .items(items)
                .build();
    }

    /**
     * Batch hisoblash: bir nechta ombor uchun productCount, lowStockCount,
     * usedVolumeM3, capacityPct va (agar since berilsa) todayIncoming ni bitta
     * o'tishda qaytaradi. N+1 oldini olish uchun ro'yxat sahifalarida ishlatiladi.
     */
    @Transactional(readOnly = true)
    public Map<UUID, WarehouseSummaryStats> summarizeWarehouses(Collection<UUID> warehouseIds, LocalDateTime since) {
        if (warehouseIds == null || warehouseIds.isEmpty()) {
            return Map.of();
        }
        List<WarehouseStock> lines = warehouseStockRepository.findAllByWarehouseIdIn(warehouseIds);
        Map<UUID, List<WarehouseStock>> byWarehouse = lines.stream()
                .collect(Collectors.groupingBy(s -> s.getWarehouse().getId()));

        Set<UUID> productIds = lines.stream().map(WarehouseStock::getProductId).collect(Collectors.toSet());
        Map<UUID, ProductReferenceService.ProductSnapshot> products = productReferenceService.fetchProducts(productIds);

        Map<UUID, Long> todayIncoming = Map.of();
        if (since != null) {
            todayIncoming = stockMovementRepository
                    .countByWarehouseIdInAndMovementTypeSince(warehouseIds, MovementType.PURCHASE_IN, since)
                    .stream()
                    .collect(Collectors.toMap(row -> (UUID) row[0], row -> ((Number) row[1]).longValue()));
        }

        Map<UUID, Warehouse> warehousesById = warehouseRepository.findAllById(warehouseIds)
                .stream()
                .collect(Collectors.toMap(Warehouse::getId, w -> w));

        Map<UUID, WarehouseSummaryStats> result = new LinkedHashMap<>();
        for (UUID warehouseId : warehouseIds) {
            List<WarehouseStock> whLines = byWarehouse.getOrDefault(warehouseId, List.of());
            int productCount = whLines.size();
            int lowStockCount = 0;
            BigDecimal used = BigDecimal.ZERO;
            for (WarehouseStock line : whLines) {
                ProductReferenceService.ProductSnapshot p = products.get(line.getProductId());
                int minStock = line.getMinStockOverride() != null
                        ? line.getMinStockOverride()
                        : (p != null ? p.minStock() : 0);
                BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
                BigDecimal reserved = line.getReservedQuantity() != null ? line.getReservedQuantity() : BigDecimal.ZERO;
                BigDecimal available = qty.subtract(reserved);
                if (available.compareTo(BigDecimal.valueOf(minStock)) <= 0) {
                    lowStockCount++;
                }
                double length = p != null ? p.length() : 0.0;
                double width = p != null ? p.width() : 0.0;
                double height = p != null ? p.height() : 0.0;
                BigDecimal unitVolumeM3 = BigDecimal.valueOf(Math.max(0.0, length) * Math.max(0.0, width) * Math.max(0.0, height))
                        .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
                used = used.add(qty.multiply(unitVolumeM3).setScale(6, RoundingMode.HALF_UP));
            }
            Warehouse warehouse = warehousesById.get(warehouseId);
            BigDecimal capacityM3 = warehouse != null && warehouse.getCapacity() != null
                    ? BigDecimal.valueOf(warehouse.getCapacity())
                    : BigDecimal.ZERO;
            int capacityPct = 0;
            if (capacityM3.compareTo(BigDecimal.ZERO) > 0) {
                capacityPct = used.multiply(BigDecimal.valueOf(100))
                        .divide(capacityM3, 2, RoundingMode.HALF_UP)
                        .min(BigDecimal.valueOf(100))
                        .intValue();
            }
            result.put(warehouseId, new WarehouseSummaryStats(
                    productCount,
                    lowStockCount,
                    todayIncoming.getOrDefault(warehouseId, 0L).intValue(),
                    used.setScale(6, RoundingMode.HALF_UP),
                    capacityPct
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public InternalWarehouseStatsResponse internalAggregateLowStock(int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold must be >= 0");
        }
        List<WarehouseStock> lines = warehouseStockRepository.findAllWithWarehouse();
        if (lines.isEmpty()) {
            return new InternalWarehouseStatsResponse(List.of(), 0);
        }

        Set<UUID> productIds = lines.stream().map(WarehouseStock::getProductId).collect(Collectors.toSet());
        Map<UUID, ProductReferenceService.VariantSnapshot> products = productReferenceService.fetchVariants(productIds);

        List<InternalWarehouseStatsResponse.LowStockItem> items = lines.stream()
                .map(line -> {
                    BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
                    BigDecimal reserved = line.getReservedQuantity() != null ? line.getReservedQuantity() : BigDecimal.ZERO;
                    BigDecimal available = qty.subtract(reserved);
                    BigDecimal bar = BigDecimal.valueOf(Math.max(0, threshold));
                    if (available.compareTo(bar) > 0) {
                        return null;
                    }
                    ProductReferenceService.VariantSnapshot p = products.get(line.getProductId());
                    String name = p != null && p.name() != null && !p.name().isBlank()
                            ? p.name()
                            : UNKNOWN_PRODUCT_NAME;
                    long availableLong = available.setScale(0, RoundingMode.DOWN).longValue();
                    return new InternalWarehouseStatsResponse.LowStockItem(
                            line.getProductId(),
                            name,
                            availableLong,
                            line.getWarehouse().getId()
                    );
                })
                .filter(i -> i != null)
                .sorted(Comparator.comparingLong(InternalWarehouseStatsResponse.LowStockItem::availableQuantity)
                        .thenComparing(InternalWarehouseStatsResponse.LowStockItem::productName, Comparator.nullsLast(String::compareTo)))
                .toList();

        return new InternalWarehouseStatsResponse(items, items.size());
    }

    @Transactional
    public WarehouseStockLineResponse upsertStock(UUID warehouseId, StockUpsertRequest body) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));
        requireProduct(body.getProductId());

        BigDecimal reserved = body.getReservedQuantity() != null
                ? body.getReservedQuantity()
                : BigDecimal.ZERO;
        if (reserved.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("reservedQuantity must be >= 0");
        }
        if (body.getQuantity().compareTo(reserved) < 0) {
            throw new IllegalArgumentException("quantity must be >= reservedQuantity");
        }

        WarehouseStock line = warehouseStockRepository
                .findByWarehouse_IdAndProductId(warehouseId, body.getProductId())
                .orElseGet(() -> {
                    WarehouseStock s = new WarehouseStock();
                    s.setWarehouse(warehouse);
                    s.setProductId(body.getProductId());
                    return s;
                });
        line.setQuantity(body.getQuantity());
        line.setReservedQuantity(reserved);
        if (body.getMinStockOverride() != null && body.getMinStockOverride() < 0) {
            throw new IllegalArgumentException("minStockOverride must be >= 0");
        }
        line.setMinStockOverride(body.getMinStockOverride());
        WarehouseStock saved = warehouseStockRepository.save(line);
        reconcileLots(warehouseId, body.getProductId());
        auditLogService.log(
                "STOCK_UPSERT",
                "WAREHOUSE_STOCK",
                warehouseId + ":" + body.getProductId(),
                warehouseId,
                "quantity=" + saved.getQuantity() + ", reservedQuantity=" + saved.getReservedQuantity()
        );
        return WarehouseStockLineResponse.fromEntity(saved);
    }

    @Transactional
    public WarehouseStockLineResponse adjustStock(UUID warehouseId, StockAdjustRequest body) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));
        requireProduct(body.getProductId());

        WarehouseStock line = warehouseStockRepository
                .findByWarehouse_IdAndProductId(warehouseId, body.getProductId())
                .orElseGet(() -> {
                    if (body.getQuantityDelta().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("No stock line for product; cannot apply non-positive delta");
                    }
                    WarehouseStock s = new WarehouseStock();
                    s.setWarehouse(warehouse);
                    s.setProductId(body.getProductId());
                    s.setQuantity(BigDecimal.ZERO);
                    s.setReservedQuantity(BigDecimal.ZERO);
                    return s;
                });

        BigDecimal reserved = line.getReservedQuantity() != null ? line.getReservedQuantity() : BigDecimal.ZERO;
        BigDecimal newQty = line.getQuantity().add(body.getQuantityDelta());
        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient quantity for delta " + body.getQuantityDelta());
        }
        if (newQty.compareTo(reserved) < 0) {
            throw new IllegalArgumentException("Resulting quantity would be below reserved quantity");
        }
        line.setQuantity(newQty);
        WarehouseStock saved = warehouseStockRepository.save(line);
        if (body.getQuantityDelta().signum() < 0) {
            stockLotService.consumeFifo(warehouseId, body.getProductId(), body.getQuantityDelta().abs());
        } else {
            stockLotService.createLot(warehouseId, body.getProductId(), body.getQuantityDelta(),
                    averageUnitCost(warehouseId, body.getProductId()), null, null, LocalDateTime.now());
        }
        stockMovementService.log(
                warehouseId,
                body.getProductId(),
                MovementType.ADJUSTMENT,
                body.getQuantityDelta(),
                null,
                "ADJUST",
                body.getReason()
        );
        auditLogService.log(
                "STOCK_ADJUST",
                "WAREHOUSE_STOCK",
                warehouseId + ":" + body.getProductId(),
                warehouseId,
                "delta=" + body.getQuantityDelta() + ", newQuantity=" + newQty
                        + (body.getReason() != null && !body.getReason().isBlank() ? ", reason=" + body.getReason() : "")
        );
        return WarehouseStockLineResponse.fromEntity(saved);
    }

    @Transactional
    public WarehouseStockLineResponse updateMinStock(UUID warehouseId, UUID productId, Integer minStockOverride) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId));
        }
        requireProduct(productId);
        if (minStockOverride != null && minStockOverride < 0) {
            throw new IllegalArgumentException("minStockOverride must be >= 0");
        }
        WarehouseStock line = warehouseStockRepository
                .findByWarehouse_IdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse stock line",
                        "warehouseId=" + warehouseId + ", productId=" + productId
                ));
        line.setMinStockOverride(minStockOverride);
        WarehouseStock saved = warehouseStockRepository.save(line);
        auditLogService.log(
                "STOCK_MIN_STOCK_UPDATE",
                "WAREHOUSE_STOCK",
                warehouseId + ":" + productId,
                warehouseId,
                "minStockOverride=" + minStockOverride
        );
        return toResponseWithSafeProductName(saved);
    }

    @Transactional
    public void addQuantity(UUID warehouseId, UUID productId, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Purchase line quantity must be positive");
        }
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));
        requireProduct(productId);

        WarehouseStock line = warehouseStockRepository
                .findByWarehouse_IdAndProductId(warehouseId, productId)
                .orElseGet(() -> {
                    WarehouseStock s = new WarehouseStock();
                    s.setWarehouse(warehouse);
                    s.setProductId(productId);
                    s.setQuantity(BigDecimal.ZERO);
                    s.setReservedQuantity(BigDecimal.ZERO);
                    return s;
                });
        line.setQuantity(line.getQuantity().add(quantity));
        warehouseStockRepository.save(line);
    }

    @Transactional
    public void subtractQuantity(UUID warehouseId, UUID productId, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        WarehouseStock line = warehouseStockRepository
                .findByWarehouse_IdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse stock line",
                        "warehouseId=" + warehouseId + ", productId=" + productId
                ));
        BigDecimal current = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
        BigDecimal reserved = line.getReservedQuantity() != null ? line.getReservedQuantity() : BigDecimal.ZERO;
        BigDecimal newQty = current.subtract(quantity);
        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResourceConflictException("Insufficient quantity for product " + productId);
        }
        if (newQty.compareTo(reserved) < 0) {
            throw new ResourceConflictException("Resulting quantity would be below reserved quantity for product " + productId);
        }
        line.setQuantity(newQty);
        warehouseStockRepository.save(line);
    }

    @Transactional
    public void deleteStockLine(UUID warehouseId, UUID productId) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId));
        }
        WarehouseStock line = warehouseStockRepository
                .findByWarehouse_IdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse stock line",
                        "warehouseId=" + warehouseId + ", productId=" + productId
                ));

        BigDecimal reserved = line.getReservedQuantity() != null ? line.getReservedQuantity() : BigDecimal.ZERO;
        if (reserved.compareTo(BigDecimal.ZERO) > 0) {
            throw new ResourceConflictException("Cannot delete stock line: product has reserved quantity");
        }

        warehouseStockRepository.delete(line);
        stockLotRepository.deleteByWarehouseIdAndProductId(warehouseId, productId);
        auditLogService.log(
                "STOCK_LINE_DELETE",
                "WAREHOUSE_STOCK",
                warehouseId + ":" + productId,
                warehouseId,
                "Deleted stock line for productId=" + productId
        );
    }

    @Transactional
    public long deleteAllByProductId(UUID productId) {
        stockLotRepository.deleteAllByProductId(productId);
        return warehouseStockRepository.deleteAllByProductId(productId);
    }


    @Transactional
    public SalesOutResponse salesOut(UUID warehouseId, SalesOutRequest request) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));
        List<SalesOutResponse.ItemResult> items = request.getItems().stream().map(i -> {
            // Pessimistic lock for concurrent safety
            WarehouseStock line = warehouseStockRepository
                    .findByWarehouseIdAndProductIdForUpdate(warehouseId, i.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Warehouse stock line",
                            "warehouseId=" + warehouseId + ", productId=" + i.getProductId()
                    ));
            BigDecimal quantity = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
            BigDecimal reserved = line.getReservedQuantity() != null ? line.getReservedQuantity() : BigDecimal.ZERO;
            BigDecimal available = quantity.subtract(reserved);
            if (available.compareTo(i.getQuantity()) < 0) {
                String productName = resolveProductNameSafely(i.getProductId());
                throw new IllegalArgumentException("INSUFFICIENT_STOCK: Product " + productName
                        + ": only " + available + " available, requested " + i.getQuantity());
            }
            BigDecimal useFromReserved = reserved.min(i.getQuantity());
            BigDecimal leftToDeduct = i.getQuantity().subtract(useFromReserved);
            line.setReservedQuantity(reserved.subtract(useFromReserved));
            line.setQuantity(quantity.subtract(leftToDeduct));
            WarehouseStock saved = warehouseStockRepository.save(line);
            // FIFO must consume exactly the amount removed from the sellable quantity
            // (leftToDeduct). Consuming the full request here breaks the invariant
            // sum(lots) == stock.quantity whenever reservedQuantity > 0.
            StockLotService.ConsumptionResult consumption = leftToDeduct.signum() > 0
                    ? stockLotService.consumeFifo(warehouseId, i.getProductId(), leftToDeduct)
                    : StockLotService.ConsumptionResult.zero();
            stockMovementService.logWithCost(warehouseId, i.getProductId(), MovementType.SALES_OUT, leftToDeduct,
                    consumption.unitCost(), consumption.totalCost(),
                    request.getOrderId(), "ORDER", null);
            auditLogService.log("STOCK_SALES_OUT", "WAREHOUSE_STOCK",
                    warehouseId + ":" + i.getProductId(), warehouseId,
                    "orderId=" + request.getOrderId() + ", deducted=" + i.getQuantity());
            return SalesOutResponse.ItemResult.builder()
                    .productId(i.getProductId())
                    .deducted(i.getQuantity())
                    .remaining(saved.getQuantity())
                    .build();
        }).toList();
        return SalesOutResponse.builder().processed(items.size()).items(items).build();
    }

    @Transactional
    public SalesOutResponse reverseSalesOut(UUID warehouseId, UUID orderId) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));

        var alreadyReversed = stockMovementRepository
                .findByWarehouseIdAndReferenceTypeAndReferenceIdAndMovementType(warehouseId, "ORDER_REVERSE", orderId, MovementType.RETURN_IN);
        if (!alreadyReversed.isEmpty()) {
            return SalesOutResponse.builder().processed(0).items(List.of()).build();
        }

        var movements = stockMovementRepository
                .findByWarehouseIdAndReferenceTypeAndReferenceIdAndMovementType(warehouseId, "ORDER", orderId, MovementType.SALES_OUT);
        if (movements.isEmpty()) {
            return SalesOutResponse.builder().processed(0).items(List.of()).build();
        }
        List<SalesOutResponse.ItemResult> items = movements.stream().map(m -> {
            WarehouseStock line = warehouseStockRepository
                    .findByWarehouseIdAndProductIdForUpdate(warehouseId, m.getProductId())
                    .orElseGet(() -> {
                        WarehouseStock s = new WarehouseStock();
                        s.setWarehouse(warehouseRepository.findById(warehouseId).orElseThrow());
                        s.setProductId(m.getProductId());
                        s.setQuantity(BigDecimal.ZERO);
                        s.setReservedQuantity(BigDecimal.ZERO);
                        return s;
                    });
            BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
            line.setQuantity(qty.add(m.getQuantity()));
            WarehouseStock saved = warehouseStockRepository.save(line);
            BigDecimal unitCost = m.getUnitCost() != null ? m.getUnitCost() : BigDecimal.ZERO;
            stockLotService.restoreAtCost(warehouseId, m.getProductId(), m.getQuantity(), unitCost);
            BigDecimal restoredCost = unitCost.multiply(m.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            stockMovementService.logWithCost(warehouseId, m.getProductId(), MovementType.RETURN_IN, m.getQuantity(),
                    unitCost, restoredCost, orderId, "ORDER_REVERSE", "sales-out-reverse");
            auditLogService.log("STOCK_SALES_OUT_REVERSE", "WAREHOUSE_STOCK",
                    warehouseId + ":" + m.getProductId(), warehouseId,
                    "orderId=" + orderId + ", restored=" + m.getQuantity());
            return SalesOutResponse.ItemResult.builder()
                    .productId(m.getProductId())
                    .deducted(m.getQuantity())
                    .remaining(saved.getQuantity())
                    .build();
        }).toList();
        return SalesOutResponse.builder().processed(items.size()).items(items).build();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<StockMovementResponse> stockHistory(
            UUID warehouseId,
            UUID productId,
            MovementType movementType,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable
    ) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId));
        }
        return stockMovementService.history(warehouseId, productId, movementType, dateFrom, dateTo, pageable);
    }

    @Transactional(readOnly = true)
    public List<StockLotResponse> listLots(UUID warehouseId, UUID productId) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId));
        }
        List<StockLot> lots;
        if (productId != null) {
            lots = stockLotRepository.findByWarehouseIdAndProductId(warehouseId, productId);
        } else {
            lots = stockLotRepository.findByWarehouseIdOrderByReceivedDateAsc(warehouseId);
        }
        Set<UUID> productIds = lots.stream().map(StockLot::getProductId).collect(Collectors.toSet());
        Map<UUID, ProductReferenceService.VariantSnapshot> products = productReferenceService.fetchVariants(productIds);
        return lots.stream()
                .map(l -> StockLotResponse.fromEntity(
                        l,
                        products.containsKey(l.getProductId())
                                ? products.get(l.getProductId()).name()
                                : UNKNOWN_PRODUCT_NAME
                ))
                .toList();
    }

    private void requireProduct(UUID productId) {
        if (!productReferenceService.existsProduct(productId)) {
            throw new ResourceNotFoundException("Product", String.valueOf(productId));
        }
    }

    private BigDecimal averageUnitCost(UUID warehouseId, UUID productId) {
        return stockLotService.averageUnitCost(warehouseId, productId);
    }

    private void reconcileLots(UUID warehouseId, UUID productId) {
        WarehouseStock line = warehouseStockRepository
                .findByWarehouse_IdAndProductId(warehouseId, productId)
                .orElse(null);
        BigDecimal target = line != null && line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
        stockLotService.reconcileToQuantity(warehouseId, productId, target);
    }

    private WarehouseStockLineResponse toResponseWithSafeProductName(WarehouseStock stock) {
        WarehouseStockLineResponse response = WarehouseStockLineResponse.fromEntity(stock);
        response.setProductName(resolveProductNameSafely(stock.getProductId()));
        response.setMinStock(resolveMinStockSafely(stock));
        BigDecimal qty = stock.getQuantity() != null ? stock.getQuantity() : BigDecimal.ZERO;
        BigDecimal unitVolume = resolveUnitVolumeM3Safely(stock.getProductId());
        response.setVolumeM3(qty.multiply(unitVolume).setScale(6, RoundingMode.HALF_UP));
        return response;
    }

    /**
     * Effective min stock: ombor override'ini o'rnatilgan bo'lsa shu, aks holda mahsulot default'i.
     * Mahsulot topilmasa 0 qaytariladi.
     */
    private int resolveMinStockSafely(WarehouseStock stock) {
        if (stock.getMinStockOverride() != null) {
            return stock.getMinStockOverride();
        }
        try {
            return productReferenceService.findProduct(stock.getProductId())
                    .map(ProductReferenceService.ProductSnapshot::minStock)
                    .orElse(0);
        } catch (RuntimeException ex) {
            log.warn("Failed to resolve minStock for productId={}. Returning 0.", stock.getProductId(), ex);
            return 0;
        }
    }

    private BigDecimal resolveUnitVolumeM3Safely(UUID productId) {
        try {
            return productReferenceService.findProduct(productId)
                    .map(p -> BigDecimal.valueOf(
                                    Math.max(0.0, p.length()) * Math.max(0.0, p.width()) * Math.max(0.0, p.height()))
                            .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP))
                    .orElse(BigDecimal.ZERO);
        } catch (RuntimeException ex) {
            log.warn("Failed to resolve dimensions for productId={}. Returning 0 volume.", productId, ex);
            return BigDecimal.ZERO;
        }
    }

    private String resolveProductNameSafely(UUID productId) {
        try {
            return productReferenceService.findVariant(productId)
                    .map(ProductReferenceService.VariantSnapshot::name)
                    .filter(name -> name != null && !name.isBlank())
                    .orElse(UNKNOWN_PRODUCT_NAME);
        } catch (RuntimeException ex) {
            log.warn("Failed to resolve product name for productId={}. Returning '{}'.",
                    productId, UNKNOWN_PRODUCT_NAME, ex);
            return UNKNOWN_PRODUCT_NAME;
        }
    }
}
