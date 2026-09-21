package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.dto.StockMovementResponse;
import mexa.club.warehouseproject.entity.MovementStatus;
import mexa.club.warehouseproject.entity.MovementType;
import mexa.club.warehouseproject.entity.StockMovement;
import mexa.club.warehouseproject.repository.StockMovementRepository;
import mexa.club.warehouseproject.security.WarehouseAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductReferenceService productReferenceService;
    private final WarehouseAccessService warehouseAccessService;

    public StockMovementService(
            StockMovementRepository stockMovementRepository,
            ProductReferenceService productReferenceService,
            WarehouseAccessService warehouseAccessService
    ) {
        this.stockMovementRepository = stockMovementRepository;
        this.productReferenceService = productReferenceService;
        this.warehouseAccessService = warehouseAccessService;
    }

    @Transactional
    public void log(
            UUID warehouseId,
            UUID productId,
            MovementType movementType,
            BigDecimal quantity,
            UUID referenceId,
            String referenceType,
            String reason
    ) {
        logWithCost(warehouseId, productId, movementType, quantity, null, null,
                referenceId, referenceType, reason);
    }

    @Transactional
    public void logWithCost(
            UUID warehouseId,
            UUID productId,
            MovementType movementType,
            BigDecimal quantity,
            BigDecimal unitCost,
            BigDecimal totalCost,
            UUID referenceId,
            String referenceType,
            String reason
    ) {
        logWithCostAndStatus(warehouseId, productId, movementType, quantity, unitCost, totalCost,
                referenceId, referenceType, reason, MovementStatus.CONFIRMED);
    }

    @Transactional
    public void logWithCostAndStatus(
            UUID warehouseId,
            UUID productId,
            MovementType movementType,
            BigDecimal quantity,
            BigDecimal unitCost,
            BigDecimal totalCost,
            UUID referenceId,
            String referenceType,
            String reason,
            MovementStatus status
    ) {
        StockMovement m = new StockMovement();
        m.setWarehouseId(warehouseId);
        m.setProductId(productId);
        m.setMovementType(movementType);
        m.setQuantity(quantity);
        m.setUnitCost(unitCost);
        m.setTotalCost(totalCost);
        m.setReferenceId(referenceId);
        m.setReferenceType(referenceType);
        m.setReason(reason);
        m.setStatus(status);
        m.setCreatedAt(LocalDateTime.now());
        m.setActorUserId(resolveCurrentUserIdOrNull());
        m.setActorUsername(resolveCurrentUsernameOrNull());
        stockMovementRepository.save(m);
    }

    @Transactional
    public void updateStatusByReference(
            UUID warehouseId,
            UUID referenceId,
            String referenceType,
            MovementType movementType,
            MovementStatus status
    ) {
        stockMovementRepository
                .findByWarehouseIdAndReferenceTypeAndReferenceIdAndMovementType(
                        warehouseId, referenceType, referenceId, movementType)
                .forEach(m -> m.setStatus(status));
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> history(
            UUID warehouseId,
            UUID productId,
            MovementType movementType,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable
    ) {
        var page = stockMovementRepository.findAll(
                stockMovementRepository.filterSpec(warehouseId, productId, movementType, dateFrom, dateTo),
                pageable);

        Map<UUID, ProductReferenceService.VariantSnapshot> products;
        try {
            Set<UUID> ids = page.getContent().stream()
                    .map(StockMovement::getProductId)
                    .collect(Collectors.toSet());
            products = productReferenceService.fetchVariants(ids);
        } catch (Exception e) {
            products = Map.of();
        }

        final Map<UUID, ProductReferenceService.VariantSnapshot> productsFinal = products;
        return page.map(m -> StockMovementResponse.fromEntity(
                m,
                productsFinal.containsKey(m.getProductId())
                        ? productsFinal.get(m.getProductId()).name()
                        : "Unknown"
        ));
    }

    private UUID resolveCurrentUserIdOrNull() {
        try {
            return warehouseAccessService.requireCurrentUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String resolveCurrentUsernameOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }
}
