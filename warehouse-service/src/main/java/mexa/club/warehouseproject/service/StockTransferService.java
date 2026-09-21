package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.dto.StockTransferResponse;
import mexa.club.warehouseproject.entity.MovementStatus;
import mexa.club.warehouseproject.entity.MovementType;
import mexa.club.warehouseproject.entity.StockTransfer;
import mexa.club.warehouseproject.entity.StockTransferStatus;
import mexa.club.warehouseproject.entity.Warehouse;
import mexa.club.warehouseproject.entity.WarehouseStock;
import mexa.club.warehouseproject.exception.ResourceConflictException;
import mexa.club.warehouseproject.exception.ResourceNotFoundException;
import mexa.club.warehouseproject.repository.StockTransferRepository;
import mexa.club.warehouseproject.repository.WarehouseRepository;
import mexa.club.warehouseproject.repository.WarehouseStockRepository;
import mexa.club.warehouseproject.security.WarehouseAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StockTransferService {

    private static final Logger log = LoggerFactory.getLogger(StockTransferService.class);

    private final StockTransferRepository stockTransferRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final WarehouseAccessService warehouseAccessService;
    private final ProductReferenceService productReferenceService;
    private final StockMovementService stockMovementService;
    private final StockLotService stockLotService;
    private final AuditLogService auditLogService;

    public StockTransferService(
            StockTransferRepository stockTransferRepository,
            WarehouseRepository warehouseRepository,
            WarehouseStockRepository warehouseStockRepository,
            WarehouseAccessService warehouseAccessService,
            ProductReferenceService productReferenceService,
            StockMovementService stockMovementService,
            StockLotService stockLotService,
            AuditLogService auditLogService
    ) {
        this.stockTransferRepository = stockTransferRepository;
        this.warehouseRepository = warehouseRepository;
        this.warehouseStockRepository = warehouseStockRepository;
        this.warehouseAccessService = warehouseAccessService;
        this.productReferenceService = productReferenceService;
        this.stockMovementService = stockMovementService;
        this.stockLotService = stockLotService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public StockTransferResponse create(UUID fromId, UUID toId, UUID productId, BigDecimal qty) {
        warehouseAccessService.requireCanManageWarehouse(fromId);
        warehouseAccessService.requireCanManageWarehouse(toId);

        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        productReferenceService.requireProductExists(productId);

        Warehouse fromWarehouse = warehouseRepository.findById(fromId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(fromId)));
        Warehouse toWarehouse = warehouseRepository.findById(toId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(toId)));

        WarehouseStock fromLine = warehouseStockRepository
                .findByWarehouse_IdAndProductId(fromId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warehouse stock line",
                        "warehouseId=" + fromId + ", productId=" + productId
                ));

        BigDecimal fromQty = fromLine.getQuantity() != null ? fromLine.getQuantity() : BigDecimal.ZERO;
        BigDecimal fromReserved = fromLine.getReservedQuantity() != null ? fromLine.getReservedQuantity() : BigDecimal.ZERO;
        if (fromQty.subtract(fromReserved).compareTo(qty) < 0) {
            throw new ResourceConflictException("Insufficient available stock");
        }

        StockLotService.ConsumptionResult consumption = stockLotService.consumeFifo(fromId, productId, qty);

        StockTransfer transfer = new StockTransfer();
        transfer.setFromWarehouseId(fromId);
        transfer.setToWarehouseId(toId);
        transfer.setProductId(productId);
        transfer.setQuantity(qty);
        transfer.setUnitCost(consumption.unitCost());
        transfer.setStatus(StockTransferStatus.PENDING);
        transfer.setCreatedBy(warehouseAccessService.requireCurrentUserId());
        transfer.setCreatedAt(LocalDateTime.now());
        stockTransferRepository.save(transfer);
        UUID transferId = transfer.getId();

        fromLine.setQuantity(fromQty.subtract(qty));
        warehouseStockRepository.save(fromLine);

        stockMovementService.logWithCost(fromId, productId, MovementType.TRANSFER_OUT, qty,
                consumption.unitCost(), consumption.totalCost(), transferId, "TRANSFER", null);
        stockMovementService.logWithCostAndStatus(toId, productId, MovementType.TRANSFER_IN, qty,
                consumption.unitCost(), consumption.totalCost(), transferId, "TRANSFER", null, MovementStatus.PENDING);

        auditLogService.log("STOCK_TRANSFER_OUT", "WAREHOUSE_STOCK", fromId + ":" + productId, fromId,
                "toWarehouseId=" + toId + ", quantity=" + qty + ", status=PENDING");
        auditLogService.log("STOCK_TRANSFER_IN", "WAREHOUSE_STOCK", toId + ":" + productId, toId,
                "fromWarehouseId=" + fromId + ", quantity=" + qty + ", status=PENDING");

        return toResponse(transfer, fromWarehouse, toWarehouse);
    }

    @Transactional
    public StockTransferResponse confirm(UUID toId, UUID transferId, String note) {
        warehouseAccessService.requireCanManageWarehouse(toId);
        StockTransfer transfer = findPendingFor(toId, transferId);

        Warehouse toWarehouse = warehouseRepository.findById(toId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(toId)));
        Warehouse fromWarehouse = warehouseRepository.findById(transfer.getFromWarehouseId()).orElse(null);

        WarehouseStock toLine = warehouseStockRepository
                .findByWarehouse_IdAndProductId(toId, transfer.getProductId())
                .orElseGet(() -> {
                    WarehouseStock s = new WarehouseStock();
                    s.setWarehouse(toWarehouse);
                    s.setProductId(transfer.getProductId());
                    s.setQuantity(BigDecimal.ZERO);
                    s.setReservedQuantity(BigDecimal.ZERO);
                    return s;
                });
        BigDecimal toQty = toLine.getQuantity() != null ? toLine.getQuantity() : BigDecimal.ZERO;
        if (toLine.getReservedQuantity() == null) {
            toLine.setReservedQuantity(BigDecimal.ZERO);
        }
        toLine.setQuantity(toQty.add(transfer.getQuantity()));
        warehouseStockRepository.save(toLine);
        stockLotService.createLot(toId, transfer.getProductId(), transfer.getQuantity(),
                transfer.getUnitCost(), null, null, LocalDateTime.now());
        stockMovementService.updateStatusByReference(
                toId, transferId, "TRANSFER", MovementType.TRANSFER_IN, MovementStatus.CONFIRMED);

        transfer.setStatus(StockTransferStatus.CONFIRMED);
        transfer.setApprovedBy(warehouseAccessService.requireCurrentUserId());
        transfer.setApprovedAt(LocalDateTime.now());
        transfer.setNote(note);
        stockTransferRepository.save(transfer);

        auditLogService.log("STOCK_TRANSFER_IN_CONFIRMED", "WAREHOUSE_STOCK",
                toId + ":" + transfer.getProductId(), toId,
                "transferId=" + transferId + ", quantity=" + transfer.getQuantity());

        return toResponse(transfer, fromWarehouse, toWarehouse);
    }

    @Transactional
    public StockTransferResponse reject(UUID toId, UUID transferId, String note) {
        warehouseAccessService.requireCanManageWarehouse(toId);
        StockTransfer transfer = findPendingFor(toId, transferId);

        Warehouse toWarehouse = warehouseRepository.findById(toId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(toId)));
        Warehouse fromWarehouse = warehouseRepository.findById(transfer.getFromWarehouseId()).orElse(null);

        WarehouseStock fromLine = warehouseStockRepository
                .findByWarehouse_IdAndProductId(transfer.getFromWarehouseId(), transfer.getProductId())
                .orElseGet(() -> {
                    WarehouseStock s = new WarehouseStock();
                    s.setWarehouse(fromWarehouse);
                    s.setProductId(transfer.getProductId());
                    s.setQuantity(BigDecimal.ZERO);
                    s.setReservedQuantity(BigDecimal.ZERO);
                    return s;
                });
        BigDecimal fromQty = fromLine.getQuantity() != null ? fromLine.getQuantity() : BigDecimal.ZERO;
        if (fromLine.getReservedQuantity() == null) {
            fromLine.setReservedQuantity(BigDecimal.ZERO);
        }
        fromLine.setQuantity(fromQty.add(transfer.getQuantity()));
        warehouseStockRepository.save(fromLine);
        stockLotService.createLot(transfer.getFromWarehouseId(), transfer.getProductId(),
                transfer.getQuantity(), transfer.getUnitCost(), null, null, LocalDateTime.now());

        stockMovementService.updateStatusByReference(
                transfer.getFromWarehouseId(), transferId, "TRANSFER", MovementType.TRANSFER_OUT, MovementStatus.CANCELLED);
        stockMovementService.updateStatusByReference(
                toId, transferId, "TRANSFER", MovementType.TRANSFER_IN, MovementStatus.CANCELLED);

        transfer.setStatus(StockTransferStatus.REJECTED);
        transfer.setApprovedBy(warehouseAccessService.requireCurrentUserId());
        transfer.setApprovedAt(LocalDateTime.now());
        transfer.setNote(note);
        stockTransferRepository.save(transfer);

        auditLogService.log("STOCK_TRANSFER_IN_REJECTED", "WAREHOUSE_STOCK",
                toId + ":" + transfer.getProductId(), toId,
                "transferId=" + transferId + ", quantity=" + transfer.getQuantity());

        return toResponse(transfer, fromWarehouse, toWarehouse);
    }

    @Transactional(readOnly = true)
    public List<StockTransferResponse> listPending(UUID toId) {
        warehouseAccessService.requireCanViewWarehouseOperations(toId);
        List<StockTransfer> transfers = stockTransferRepository
                .findByToWarehouseIdAndStatusOrderByCreatedAtDesc(toId, StockTransferStatus.PENDING);
        return toResponses(transfers);
    }

    private StockTransfer findPendingFor(UUID toId, UUID transferId) {
        StockTransfer transfer = stockTransferRepository
                .findByIdAndToWarehouseId(transferId, toId)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransfer", String.valueOf(transferId)));
        if (transfer.getStatus() != StockTransferStatus.PENDING) {
            throw new ResourceConflictException(
                    "Transfer is already " + transfer.getStatus().name().toLowerCase());
        }
        return transfer;
    }

    private List<StockTransferResponse> toResponses(List<StockTransfer> transfers) {
        Map<UUID, Warehouse> warehouses = warehouseRepository
                .findAllById(transfers.stream()
                        .flatMap(t -> java.util.stream.Stream.of(t.getFromWarehouseId(), t.getToWarehouseId()))
                        .collect(java.util.stream.Collectors.toSet()))
                .stream()
                .collect(java.util.stream.Collectors.toMap(Warehouse::getId, w -> w));
        return transfers.stream()
                .map(t -> toResponse(t,
                        warehouses.get(t.getFromWarehouseId()),
                        warehouses.get(t.getToWarehouseId())))
                .toList();
    }

    private StockTransferResponse toResponse(StockTransfer t, Warehouse from, Warehouse to) {
        String productName = "Unknown";
        try {
            productName = productReferenceService.findVariant(t.getProductId())
                    .map(ProductReferenceService.VariantSnapshot::name)
                    .orElse("Unknown");
        } catch (Exception e) {
            log.debug("Failed to resolve product name for {}", t.getProductId(), e);
        }
        return StockTransferResponse.fromEntity(t,
                from != null ? from.getName() : "Unknown",
                to != null ? to.getName() : "Unknown",
                productName);
    }
}
