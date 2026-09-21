package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.dto.PurchaseCreateRequest;
import mexa.club.warehouseproject.dto.PurchaseItemRequest;
import mexa.club.warehouseproject.dto.PurchaseItemUpdateRequest;
import mexa.club.warehouseproject.dto.PurchaseResponse;
import mexa.club.warehouseproject.dto.PurchaseUpdateRequest;
import mexa.club.warehouseproject.entity.Purchase;
import mexa.club.warehouseproject.entity.PurchaseItem;
import mexa.club.warehouseproject.entity.Warehouse;
import mexa.club.warehouseproject.entity.MovementType;
import mexa.club.warehouseproject.exception.ResourceNotFoundException;
import mexa.club.warehouseproject.repository.PurchaseRepository;
import mexa.club.warehouseproject.repository.WarehouseRepository;
import mexa.club.warehouseproject.repository.WarehouseStockRepository;
import mexa.club.warehouseproject.security.WarehouseAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PurchaseService {

    private static final String DEFAULT_SUPPLIER_NAME = "Noma'lum yetkazib beruvchi";

    private final PurchaseRepository purchaseRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductReferenceService productReferenceService;
    private final WarehouseStockRepository warehouseStockRepository;
    private final WarehouseStockService warehouseStockService;
    private final StockLotService stockLotService;
    private final WarehouseAccessService warehouseAccessService;
    private final AuditLogService auditLogService;
    private final WarehouseReportService warehouseReportService;
    private final StockMovementService stockMovementService;

    public PurchaseService(
            PurchaseRepository purchaseRepository,
            WarehouseRepository warehouseRepository,
            ProductReferenceService productReferenceService,
            WarehouseStockRepository warehouseStockRepository,
            WarehouseStockService warehouseStockService,
            StockLotService stockLotService,
            WarehouseAccessService warehouseAccessService,
            AuditLogService auditLogService,
            WarehouseReportService warehouseReportService,
            StockMovementService stockMovementService
    ) {
        this.purchaseRepository = purchaseRepository;
        this.warehouseRepository = warehouseRepository;
        this.productReferenceService = productReferenceService;
        this.warehouseStockRepository = warehouseStockRepository;
        this.warehouseStockService = warehouseStockService;
        this.stockLotService = stockLotService;
        this.warehouseAccessService = warehouseAccessService;
        this.auditLogService = auditLogService;
        this.warehouseReportService = warehouseReportService;
        this.stockMovementService = stockMovementService;
    }

    @Transactional(readOnly = true)
    public Page<PurchaseResponse> listPurchases(UUID warehouseId, Pageable pageable) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId));
        }
        return purchaseRepository.findByWarehouseId(warehouseId, pageable).map(PurchaseResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public PurchaseResponse getPurchase(UUID warehouseId, UUID purchaseId) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        Purchase p = purchaseRepository.findByIdAndWarehouseId(purchaseId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", String.valueOf(purchaseId)));
        return PurchaseResponse.fromEntity(p);
    }

    @Transactional
    public PurchaseResponse createPurchase(UUID warehouseId, PurchaseCreateRequest dto) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));

        Purchase purchase = new Purchase();
        purchase.setWarehouse(warehouse);
        purchase.setPurchaseDate(dto.getPurchaseDate() != null ? dto.getPurchaseDate() : LocalDateTime.now());
        purchase.setInvoiceNumber(trimToNull(dto.getInvoiceNumber()));

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseItemRequest line : dto.getItems()) {
            productReferenceService.requireProductExists(line.getProductId());
            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setProductId(line.getProductId());
            item.setQuantity(line.getQuantity());
            item.setUnitPrice(line.getUnitPrice());
            BigDecimal lineTotal = line.getQuantity().multiply(line.getUnitPrice())
                    .setScale(6, RoundingMode.HALF_UP);
            item.setTotalPrice(lineTotal);
            purchase.getItems().add(item);
            total = total.add(lineTotal);
        }
        purchase.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));

        Purchase saved = purchaseRepository.save(purchase);

        for (PurchaseItem item : saved.getItems()) {
            warehouseStockService.addQuantity(warehouseId, item.getProductId(), item.getQuantity());
            stockLotService.createLot(
                    warehouseId,
                    item.getProductId(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    saved.getId(),
                    item.getId(),
                    saved.getPurchaseDate()
            );
            stockMovementService.log(
                    warehouseId,
                    item.getProductId(),
                    MovementType.PURCHASE_IN,
                    item.getQuantity().abs(),
                    saved.getId(),
                    "PURCHASE",
                    "purchase-create"
            );
        }

        return PurchaseResponse.fromEntity(saved);
    }

    @Transactional
    public PurchaseResponse updatePurchaseItem(
            UUID warehouseId,
            UUID purchaseId,
            UUID itemId,
            PurchaseItemUpdateRequest dto
    ) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        Purchase purchase = purchaseRepository.findByIdAndWarehouseId(purchaseId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", String.valueOf(purchaseId)));

        PurchaseItem item = purchase.getItems().stream()
                .filter(i -> itemId.equals(i.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Purchase item", String.valueOf(itemId)));

        UUID newProductId = dto.getProductId();
        productReferenceService.requireProductExists(newProductId);

        UUID oldProductId = item.getProductId();
        BigDecimal oldQty = item.getQuantity();
        BigDecimal newQty = dto.getQuantity();

        if (oldProductId.equals(newProductId)) {
            BigDecimal delta = newQty.subtract(oldQty);
            if (delta.signum() != 0) {
                if (delta.compareTo(BigDecimal.ZERO) > 0) {
                    warehouseStockService.addQuantity(warehouseId, oldProductId, delta);
                    stockLotService.growPurchaseItemLot(itemId, delta);
                } else {
                    BigDecimal reduce = delta.abs();
                    warehouseStockService.subtractQuantity(warehouseId, oldProductId, reduce);
                    stockLotService.rollbackPurchaseItem(warehouseId, oldProductId, itemId, reduce);
                }
            }
        } else {
            warehouseStockService.subtractQuantity(warehouseId, oldProductId, oldQty);
            stockLotService.rollbackPurchaseItem(warehouseId, oldProductId, itemId, oldQty);
            warehouseStockService.addQuantity(warehouseId, newProductId, newQty);
            stockLotService.createLot(warehouseId, newProductId, newQty, dto.getUnitPrice(),
                    purchaseId, itemId, purchase.getPurchaseDate());
        }

        item.setProductId(newProductId);
        item.setQuantity(newQty);
        item.setUnitPrice(dto.getUnitPrice());
        item.setTotalPrice(newQty.multiply(dto.getUnitPrice()).setScale(6, RoundingMode.HALF_UP));

        recomputePurchaseTotal(purchase);
        Purchase saved = purchaseRepository.save(purchase);
        return PurchaseResponse.fromEntity(saved);
    }

    @Transactional
    public PurchaseResponse deletePurchaseItem(UUID warehouseId, UUID purchaseId, UUID itemId) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        Purchase purchase = purchaseRepository.findByIdAndWarehouseId(purchaseId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", String.valueOf(purchaseId)));

        Optional<PurchaseItem> match = purchase.getItems().stream()
                .filter(i -> itemId.equals(i.getId()))
                .findFirst();
        PurchaseItem item = match.orElseThrow(() -> new ResourceNotFoundException("Purchase item", String.valueOf(itemId)));

        warehouseStockService.subtractQuantity(warehouseId, item.getProductId(), item.getQuantity());
        stockLotService.rollbackPurchaseItem(warehouseId, item.getProductId(), itemId, item.getQuantity());
        purchase.getItems().remove(item);

        recomputePurchaseTotal(purchase);
        Purchase saved = purchaseRepository.save(purchase);
        return PurchaseResponse.fromEntity(saved);
    }

    @Transactional
    public PurchaseResponse updatePurchaseHeader(UUID warehouseId, UUID purchaseId, PurchaseUpdateRequest dto) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        Purchase purchase = purchaseRepository.findByIdAndWarehouseId(purchaseId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", String.valueOf(purchaseId)));

        if (dto.getInvoiceNumber() != null) {
            purchase.setInvoiceNumber(trimToNull(dto.getInvoiceNumber()));
        }
        if (dto.getPurchaseDate() != null) {
            purchase.setPurchaseDate(dto.getPurchaseDate());
        }
        Purchase saved = purchaseRepository.save(purchase);
        return PurchaseResponse.fromEntity(saved);
    }

    @Transactional
    public void deletePurchase(UUID warehouseId, UUID purchaseId) {
        Purchase purchase = purchaseRepository.findByIdAndWarehouseId(purchaseId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", String.valueOf(purchaseId)));

        for (PurchaseItem item : purchase.getItems()) {
            warehouseStockService.subtractQuantity(warehouseId, item.getProductId(), item.getQuantity());
            stockLotService.rollbackPurchaseItem(warehouseId, item.getProductId(), item.getId(), item.getQuantity());
        }

        stockLotService.removeLotsForPurchase(purchaseId);
        purchaseRepository.delete(purchase);
        auditLogService.log(
                "PURCHASE_DELETE",
                "PURCHASE",
                String.valueOf(purchaseId),
                warehouseId,
                "Deleted purchase and rolled back stock for " + purchase.getItems().size() + " item(s)"
        );
    }

    private static void recomputePurchaseTotal(Purchase purchase) {
        BigDecimal sum = purchase.getItems().stream()
                .map(PurchaseItem::getTotalPrice)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        purchase.setTotalAmount(sum.setScale(2, RoundingMode.HALF_UP));
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> purchasePdf(UUID warehouseId, UUID purchaseId) {
        return warehouseReportService.purchasePdf(warehouseId, purchaseId);
    }
}
