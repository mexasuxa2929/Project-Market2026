package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.dto.StockReturnCreateRequest;
import mexa.club.warehouseproject.dto.StockReturnResponse;
import mexa.club.warehouseproject.entity.MovementType;
import mexa.club.warehouseproject.entity.ReturnCondition;
import mexa.club.warehouseproject.entity.StockMovement;
import mexa.club.warehouseproject.entity.StockReturn;
import mexa.club.warehouseproject.entity.StockReturnItem;
import mexa.club.warehouseproject.entity.StockReturnStatus;
import mexa.club.warehouseproject.entity.Warehouse;
import mexa.club.warehouseproject.entity.WarehouseStock;
import mexa.club.warehouseproject.exception.ResourceNotFoundException;
import mexa.club.warehouseproject.repository.StockMovementRepository;
import mexa.club.warehouseproject.repository.StockReturnRepository;
import mexa.club.warehouseproject.repository.WarehouseRepository;
import mexa.club.warehouseproject.repository.WarehouseStockRepository;
import mexa.club.warehouseproject.security.WarehouseAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StockReturnService {

    private final StockReturnRepository stockReturnRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final WarehouseAccessService warehouseAccessService;
    private final AuditLogService auditLogService;
    private final StockMovementService stockMovementService;
    private final StockLotService stockLotService;
    private final ProductReferenceService productReferenceService;

    public StockReturnService(
            StockReturnRepository stockReturnRepository,
            StockMovementRepository stockMovementRepository,
            WarehouseRepository warehouseRepository,
            WarehouseStockRepository warehouseStockRepository,
            WarehouseAccessService warehouseAccessService,
            AuditLogService auditLogService,
            StockMovementService stockMovementService,
            StockLotService stockLotService,
            ProductReferenceService productReferenceService
    ) {
        this.stockReturnRepository = stockReturnRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.warehouseRepository = warehouseRepository;
        this.warehouseStockRepository = warehouseStockRepository;
        this.warehouseAccessService = warehouseAccessService;
        this.auditLogService = auditLogService;
        this.stockMovementService = stockMovementService;
        this.stockLotService = stockLotService;
        this.productReferenceService = productReferenceService;
    }

    @Transactional
    public StockReturnResponse create(UUID warehouseId, StockReturnCreateRequest request) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));
        StockReturn sr = new StockReturn();
        sr.setWarehouseId(warehouseId);
        sr.setOrderId(request.getOrderId());
        sr.setReason(request.getReason());
        sr.setNote(request.getNote());
        sr.setCreatedAt(LocalDateTime.now());
        sr.setReturnedBy(warehouseAccessService.requireCurrentUserId());
        sr.setStatus(StockReturnStatus.PENDING);
        for (var it : request.getItems()) {
            StockReturnItem item = new StockReturnItem();
            item.setStockReturn(sr);
            item.setProductId(it.getProductId());
            item.setQuantity(it.getQuantity());
            item.setCondition(it.getCondition());
            sr.getItems().add(item);
        }
        StockReturn saved = stockReturnRepository.save(sr);
        auditLogService.log("STOCK_RETURN_CREATE", "STOCK_RETURN", String.valueOf(saved.getId()), warehouseId, "reason=" + request.getReason());
        return enrich(StockReturnResponse.fromEntity(saved));
    }

    @Transactional(readOnly = true)
    public Page<StockReturnResponse> list(UUID warehouseId, Pageable pageable) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        return stockReturnRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId, pageable)
                .map(sr -> enrich(StockReturnResponse.fromEntity(sr)));
    }

    @Transactional(readOnly = true)
    public StockReturnResponse get(UUID warehouseId, UUID id) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        StockReturn sr = stockReturnRepository.findByIdAndWarehouseId(id, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("StockReturn", String.valueOf(id)));
        return enrich(StockReturnResponse.fromEntity(sr));
    }

    @Transactional
    public StockReturnResponse approve(UUID warehouseId, UUID id, String note) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        StockReturn sr = stockReturnRepository.findByIdAndWarehouseIdForUpdate(id, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("StockReturn", String.valueOf(id)));
        if (sr.getStatus() != StockReturnStatus.PENDING) {
            throw new IllegalArgumentException("Return already processed");
        }
        if (sr.getOrderId() != null) {
            validateReturnWithinOrderLimits(warehouseId, sr);
        }
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));
        for (StockReturnItem item : sr.getItems()) {
            if (item.getCondition() == ReturnCondition.GOOD) {
                WarehouseStock stock = warehouseStockRepository
                        .findByWarehouseIdAndProductIdForUpdate(warehouseId, item.getProductId())
                        .orElseGet(() -> {
                            WarehouseStock s = new WarehouseStock();
                            s.setWarehouse(warehouse);
                            s.setProductId(item.getProductId());
                            s.setQuantity(BigDecimal.ZERO);
                            s.setReservedQuantity(BigDecimal.ZERO);
                            return s;
                        });
                stock.setQuantity((stock.getQuantity() != null ? stock.getQuantity() : BigDecimal.ZERO).add(item.getQuantity()));
                warehouseStockRepository.save(stock);
                BigDecimal unitCost = resolveReturnUnitCost(warehouseId, sr, item);
                stockLotService.restoreAtCost(warehouseId, item.getProductId(), item.getQuantity(), unitCost);
                stockMovementService.logWithCost(warehouseId, item.getProductId(), MovementType.RETURN_IN,
                        item.getQuantity(), unitCost,
                        unitCost.multiply(item.getQuantity()).setScale(2, RoundingMode.HALF_UP),
                        sr.getId(), "RETURN", sr.getReason());
            }
        }
        sr.setStatus(StockReturnStatus.APPROVED);
        sr.setApprovedAt(LocalDateTime.now());
        sr.setApprovedBy(warehouseAccessService.requireCurrentUserId());
        if (note != null && !note.isBlank()) {
            sr.setNote(note.trim());
        }
        StockReturn saved = stockReturnRepository.save(sr);
        auditLogService.log("STOCK_RETURN", "STOCK_RETURN", String.valueOf(saved.getId()), warehouseId, "approved");
        return enrich(StockReturnResponse.fromEntity(saved));
    }

    @Transactional
    public StockReturnResponse reject(UUID warehouseId, UUID id, String note) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        StockReturn sr = stockReturnRepository.findByIdAndWarehouseIdForUpdate(id, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("StockReturn", String.valueOf(id)));
        if (sr.getStatus() != StockReturnStatus.PENDING) {
            throw new IllegalArgumentException("Return already processed");
        }
        sr.setStatus(StockReturnStatus.REJECTED);
        if (note != null && !note.isBlank()) {
            sr.setNote(note.trim());
        }
        StockReturn saved = stockReturnRepository.save(sr);
        auditLogService.log("STOCK_RETURN", "STOCK_RETURN", String.valueOf(saved.getId()), warehouseId, "rejected");
        return enrich(StockReturnResponse.fromEntity(saved));
    }

    private StockReturnResponse enrich(StockReturnResponse response) {
        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            return response;
        }
        Set<UUID> productIds = response.getItems().stream()
                .map(StockReturnResponse.Item::getProductId)
                .collect(Collectors.toSet());
        Map<UUID, ProductReferenceService.VariantSnapshot> products;
        try {
            products = productReferenceService.fetchVariants(productIds);
        } catch (Exception e) {
            products = Map.of();
        }
        for (StockReturnResponse.Item item : response.getItems()) {
            ProductReferenceService.VariantSnapshot p = products.get(item.getProductId());
            item.setProductName(p != null && p.name() != null ? p.name() : "Unknown");
        }
        return response;
    }

    private void validateReturnWithinOrderLimits(UUID warehouseId, StockReturn sr) {
        UUID orderId = sr.getOrderId();
        for (StockReturnItem item : sr.getItems()) {
            if (item.getCondition() != ReturnCondition.GOOD) {
                continue;
            }
            BigDecimal sold = sumMovementForProduct(warehouseId, "ORDER", orderId, MovementType.SALES_OUT, item.getProductId());
            BigDecimal autoReversed = sumMovementForProduct(warehouseId, "ORDER_REVERSE", orderId, MovementType.RETURN_IN, item.getProductId());
            BigDecimal soldStillOut = sold.subtract(autoReversed).max(BigDecimal.ZERO);
            BigDecimal alreadyReturned = sumReturnedForOrder(warehouseId, orderId, item.getProductId());
            if (alreadyReturned.add(item.getQuantity()).compareTo(soldStillOut) > 0) {
                throw new IllegalArgumentException(
                        "Qaytarilayotgan miqdor sotilgan miqdordan oshib ketdi: "
                                + "product=" + item.getProductId()
                                + ", sotilgan=" + soldStillOut
                                + ", allaqachon qaytarilgan=" + alreadyReturned
                                + ", so'ralgan=" + item.getQuantity());
            }
        }
    }

    private BigDecimal sumMovementForProduct(UUID warehouseId, String referenceType, UUID referenceId,
                                             MovementType movementType, UUID productId) {
        return stockMovementRepository
                .findByWarehouseIdAndReferenceTypeAndReferenceIdAndMovementType(
                        warehouseId, referenceType, referenceId, movementType)
                .stream()
                .filter(m -> productId.equals(m.getProductId()))
                .map(m -> m.getQuantity() != null ? m.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumReturnedForOrder(UUID warehouseId, UUID orderId, UUID productId) {
        return stockReturnRepository.findByWarehouseIdAndOrderIdAndStatus(warehouseId, orderId, StockReturnStatus.APPROVED)
                .stream()
                .flatMap(r -> r.getItems().stream())
                .filter(i -> i.getCondition() == ReturnCondition.GOOD && productId.equals(i.getProductId()))
                .map(i -> i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal resolveReturnUnitCost(UUID warehouseId, StockReturn sr, StockReturnItem item) {
        if (sr.getOrderId() != null) {
            List<StockMovement> sales = stockMovementRepository
                    .findByWarehouseIdAndReferenceTypeAndReferenceIdAndMovementType(
                            warehouseId, "ORDER", sr.getOrderId(), MovementType.SALES_OUT)
                    .stream()
                    .filter(m -> item.getProductId().equals(m.getProductId()))
                    .toList();
            BigDecimal totalQty = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            for (StockMovement m : sales) {
                BigDecimal qty = m.getQuantity() != null ? m.getQuantity() : BigDecimal.ZERO;
                totalQty = totalQty.add(qty);
                totalCost = totalCost.add(qty.multiply(m.getUnitCost() != null ? m.getUnitCost() : BigDecimal.ZERO));
            }
            if (totalQty.signum() > 0) {
                return totalCost.divide(totalQty, 6, RoundingMode.HALF_UP);
            }
        }
        return stockLotService.averageUnitCost(warehouseId, item.getProductId());
    }
}
