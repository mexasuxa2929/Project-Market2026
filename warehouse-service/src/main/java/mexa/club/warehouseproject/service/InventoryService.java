package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.dto.InventoryCountRequest;
import mexa.club.warehouseproject.dto.InventoryCreateRequest;
import mexa.club.warehouseproject.dto.InventorySessionResponse;
import mexa.club.warehouseproject.entity.InventoryItem;
import mexa.club.warehouseproject.entity.InventoryItemStatus;
import mexa.club.warehouseproject.entity.InventorySession;
import mexa.club.warehouseproject.entity.InventoryStatus;
import mexa.club.warehouseproject.entity.MovementType;
import mexa.club.warehouseproject.entity.WarehouseStock;
import mexa.club.warehouseproject.exception.ResourceNotFoundException;
import mexa.club.warehouseproject.repository.InventoryItemRepository;
import mexa.club.warehouseproject.repository.InventorySessionRepository;
import mexa.club.warehouseproject.repository.WarehouseRepository;
import mexa.club.warehouseproject.repository.WarehouseStockRepository;
import mexa.club.warehouseproject.security.WarehouseAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventorySessionRepository inventorySessionRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final WarehouseAccessService warehouseAccessService;
    private final AuditLogService auditLogService;
    private final StockMovementService stockMovementService;
    private final ProductReferenceService productReferenceService;
    private final StockLotService stockLotService;

    public InventoryService(
            InventorySessionRepository inventorySessionRepository,
            InventoryItemRepository inventoryItemRepository,
            WarehouseRepository warehouseRepository,
            WarehouseStockRepository warehouseStockRepository,
            WarehouseAccessService warehouseAccessService,
            AuditLogService auditLogService,
            StockMovementService stockMovementService,
            ProductReferenceService productReferenceService,
            StockLotService stockLotService
    ) {
        this.inventorySessionRepository = inventorySessionRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.warehouseRepository = warehouseRepository;
        this.warehouseStockRepository = warehouseStockRepository;
        this.warehouseAccessService = warehouseAccessService;
        this.auditLogService = auditLogService;
        this.stockMovementService = stockMovementService;
        this.productReferenceService = productReferenceService;
        this.stockLotService = stockLotService;
    }

    @Transactional
    public InventorySessionResponse start(UUID warehouseId, InventoryCreateRequest request) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));
        // Race condition fix: use native query to atomically check+insert
        int existing = inventorySessionRepository.countActiveSessions(warehouseId);
        if (existing > 0) {
            throw new IllegalArgumentException("Only one OPEN/IN_PROGRESS inventory session allowed per warehouse");
        }
        InventorySession session = new InventorySession();
        session.setWarehouseId(warehouseId);
        session.setStatus(InventoryStatus.OPEN);
        session.setStartedBy(warehouseAccessService.requireCurrentUserId());
        session.setStartedAt(LocalDateTime.now());
        session.setNote(request != null ? request.getNote() : null);
        List<WarehouseStock> stocks = warehouseStockRepository.findAllByWarehouse_Id(warehouseId);
        for (WarehouseStock stock : stocks) {
            InventoryItem item = new InventoryItem();
            item.setSession(session);
            item.setProductId(stock.getProductId());
            item.setSystemQuantity(stock.getQuantity() != null ? stock.getQuantity() : BigDecimal.ZERO);
            item.setCountedQuantity(null);
            item.setDifference(null);
            item.setStatus(InventoryItemStatus.PENDING);
            session.getItems().add(item);
        }
        InventorySession saved = inventorySessionRepository.save(session);
        auditLogService.log("INVENTORY_START", "INVENTORY_SESSION", String.valueOf(saved.getId()), warehouseId, "items=" + saved.getItems().size());
        return enrich(InventorySessionResponse.fromEntity(saved));
    }

    @Transactional(readOnly = true)
    public Page<InventorySessionResponse> list(UUID warehouseId, Pageable pageable) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        return inventorySessionRepository.findByWarehouseIdOrderByStartedAtDesc(warehouseId, pageable)
                .map(session -> enrich(InventorySessionResponse.fromEntity(session)));
    }

    @Transactional(readOnly = true)
    public InventorySessionResponse get(UUID warehouseId, UUID sessionId) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        InventorySession s = inventorySessionRepository.findByIdAndWarehouseId(sessionId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("InventorySession", String.valueOf(sessionId)));
        return enrich(InventorySessionResponse.fromEntity(s));
    }

    @Transactional
    public InventorySessionResponse countItem(UUID warehouseId, UUID sessionId, UUID itemId, InventoryCountRequest request) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        InventorySession session = inventorySessionRepository.findByIdAndWarehouseId(sessionId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("InventorySession", String.valueOf(sessionId)));
        if (session.getStatus() == InventoryStatus.COMPLETED || session.getStatus() == InventoryStatus.CANCELLED) {
            throw new IllegalArgumentException("Session already closed");
        }
        if (session.getStatus() == InventoryStatus.OPEN) {
            session.setStatus(InventoryStatus.IN_PROGRESS);
        }
        InventoryItem item = inventoryItemRepository.findByIdAndSession_Id(itemId, sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", String.valueOf(itemId)));
        item.setCountedQuantity(request.getCountedQuantity());
        item.setDifference(request.getCountedQuantity().subtract(item.getSystemQuantity()));
        item.setStatus(InventoryItemStatus.COUNTED);
        inventoryItemRepository.save(item);
        inventorySessionRepository.save(session);
        return enrich(InventorySessionResponse.fromEntity(session));
    }

    @Transactional
    public InventorySessionResponse complete(UUID warehouseId, UUID sessionId) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        InventorySession session = inventorySessionRepository.findByIdAndWarehouseId(sessionId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("InventorySession", String.valueOf(sessionId)));
        if (session.getStatus() == InventoryStatus.CANCELLED || session.getStatus() == InventoryStatus.COMPLETED) {
            throw new IllegalArgumentException("Session already closed");
        }
        // Check that at least one item was counted
        long countedCount = session.getItems().stream()
                .filter(item -> item.getCountedQuantity() != null)
                .count();
        if (countedCount == 0) {
            throw new IllegalArgumentException("No items have been counted yet. Count at least one item before completing.");
        }
        for (InventoryItem item : session.getItems()) {
            if (item.getCountedQuantity() == null) {
                continue;
            }
            BigDecimal diff = item.getCountedQuantity().subtract(item.getSystemQuantity());
            item.setDifference(diff);
            item.setStatus(InventoryItemStatus.APPROVED);
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                // Race condition fix: use pessimistic lock on stock row
                WarehouseStock stock = warehouseStockRepository
                        .findByWarehouseIdAndProductIdForUpdate(warehouseId, item.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Warehouse stock line",
                                "productId=" + item.getProductId()));
                stock.setQuantity(item.getCountedQuantity());
                warehouseStockRepository.save(stock);
                stockLotService.reconcileToQuantity(warehouseId, item.getProductId(), item.getCountedQuantity());
                stockMovementService.log(warehouseId, item.getProductId(), MovementType.INVENTORY, diff, sessionId, "INVENTORY", "inventory-adjustment");
                String productName = productReferenceService.findVariant(item.getProductId())
                        .map(ProductReferenceService.VariantSnapshot::name).orElse("Unknown");
                auditLogService.log(
                        "INVENTORY_ADJUST",
                        "WAREHOUSE_STOCK",
                        warehouseId + ":" + item.getProductId(),
                        warehouseId,
                        "Inventory: product " + productName + " system=" + item.getSystemQuantity() + " counted=" + item.getCountedQuantity() + " diff=" + diff
                );
            }
        }
        session.setStatus(InventoryStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        session.setCompletedBy(warehouseAccessService.requireCurrentUserId());
        InventorySession saved = inventorySessionRepository.save(session);
        auditLogService.log("INVENTORY_COMPLETE", "INVENTORY_SESSION", String.valueOf(saved.getId()), warehouseId, "completed");
        return enrich(InventorySessionResponse.fromEntity(saved));
    }

    @Transactional
    public InventorySessionResponse cancel(UUID warehouseId, UUID sessionId) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        InventorySession session = inventorySessionRepository.findByIdAndWarehouseId(sessionId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("InventorySession", String.valueOf(sessionId)));
        if (!(session.getStatus() == InventoryStatus.OPEN || session.getStatus() == InventoryStatus.IN_PROGRESS)) {
            throw new IllegalArgumentException("Only OPEN or IN_PROGRESS sessions can be cancelled");
        }
        session.setStatus(InventoryStatus.CANCELLED);
        // Fix: do NOT write completedAt/completedBy for cancelled sessions
        InventorySession saved = inventorySessionRepository.save(session);
        auditLogService.log("INVENTORY_CANCEL", "INVENTORY_SESSION", String.valueOf(saved.getId()), warehouseId, "cancelled");
        return enrich(InventorySessionResponse.fromEntity(saved));
    }

    private InventorySessionResponse enrich(InventorySessionResponse response) {
        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            return response;
        }
        Set<UUID> productIds = response.getItems().stream()
                .map(InventorySessionResponse.Item::getProductId)
                .collect(Collectors.toSet());
        Map<UUID, ProductReferenceService.VariantSnapshot> products = productReferenceService.fetchVariants(productIds);
        for (InventorySessionResponse.Item item : response.getItems()) {
            ProductReferenceService.VariantSnapshot p = products.get(item.getProductId());
            item.setProductName(p != null && p.name() != null ? p.name() : "Unknown");
        }
        return response;
    }
}
