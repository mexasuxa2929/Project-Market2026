package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.dto.WarehouseAdminsRequest;
import mexa.club.warehouseproject.dto.WarehouseCreateRequest;
import mexa.club.warehouseproject.dto.WarehouseResponse;
import mexa.club.warehouseproject.entity.Warehouse;
import mexa.club.warehouseproject.entity.WarehouseAdmin;
import mexa.club.warehouseproject.client.GeoServiceClient;
import mexa.club.warehouseproject.exception.ResourceConflictException;
import mexa.club.warehouseproject.exception.ResourceNotFoundException;
import mexa.club.warehouseproject.repository.AuditLogRepository;
import mexa.club.warehouseproject.repository.InventorySessionRepository;
import mexa.club.warehouseproject.repository.PurchaseRepository;
import mexa.club.warehouseproject.repository.StockLotRepository;
import mexa.club.warehouseproject.repository.StockMovementRepository;
import mexa.club.warehouseproject.repository.StockReturnRepository;
import mexa.club.warehouseproject.repository.WarehouseAdminRepository;
import mexa.club.warehouseproject.repository.WarehouseRepository;
import mexa.club.warehouseproject.repository.WarehouseStockRepository;
import mexa.club.warehouseproject.realtime.RealtimeEventPublisher;
import mexa.club.warehouseproject.security.WarehouseAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final PurchaseRepository purchaseRepository;
    private final StockLotRepository stockLotRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockReturnRepository stockReturnRepository;
    private final InventorySessionRepository inventorySessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final WarehouseAdminRepository warehouseAdminRepository;
    private final WarehouseAccessService warehouseAccessService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final GeoServiceClient geoServiceClient;
    private final WarehouseStockService warehouseStockService;

    public WarehouseService(
            WarehouseRepository warehouseRepository,
            WarehouseStockRepository warehouseStockRepository,
            PurchaseRepository purchaseRepository,
            StockLotRepository stockLotRepository,
            StockMovementRepository stockMovementRepository,
            StockReturnRepository stockReturnRepository,
            InventorySessionRepository inventorySessionRepository,
            AuditLogRepository auditLogRepository,
            WarehouseAdminRepository warehouseAdminRepository,
            WarehouseAccessService warehouseAccessService,
            RealtimeEventPublisher realtimeEventPublisher,
            GeoServiceClient geoServiceClient,
            WarehouseStockService warehouseStockService
    ) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseStockRepository = warehouseStockRepository;
        this.purchaseRepository = purchaseRepository;
        this.stockLotRepository = stockLotRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.stockReturnRepository = stockReturnRepository;
        this.inventorySessionRepository = inventorySessionRepository;
        this.auditLogRepository = auditLogRepository;
        this.warehouseAdminRepository = warehouseAdminRepository;
        this.warehouseAccessService = warehouseAccessService;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.geoServiceClient = geoServiceClient;
        this.warehouseStockService = warehouseStockService;
    }

    @Transactional(readOnly = true)
    public Page<WarehouseResponse> listWarehouses(Pageable pageable) {
        Page<Warehouse> page;
        if (warehouseAccessService.hasSuperAdminRole()) {
            page = warehouseRepository.findAllWithAdmins(pageable);
        } else if (warehouseAccessService.hasWarehouseAdminRole()) {
            UUID uid = warehouseAccessService.requireCurrentUserId();
            page = warehouseRepository.findByAdminUserId(uid, pageable);
        } else {
            page = warehouseRepository.findAllWithAdmins(pageable);
        }

        List<UUID> ids = page.getContent().stream().map(Warehouse::getId).toList();
        Map<UUID, WarehouseStockService.WarehouseSummaryStats> stats = ids.isEmpty()
                ? Map.of()
                : warehouseStockService.summarizeWarehouses(ids, LocalDate.now().atStartOfDay());

        return page.map(w -> {
            WarehouseResponse r = WarehouseResponse.fromEntity(w);
            WarehouseStockService.WarehouseSummaryStats s = stats.get(w.getId());
            if (s != null) {
                r.setProductCount(s.productCount());
                r.setLowStockCount(s.lowStockCount());
                r.setTodayIncoming(s.todayIncoming());
                r.setUsedVolumeM3(s.usedVolumeM3());
                r.setCapacityPct(s.capacityPct());
            } else {
                r.setProductCount(0);
                r.setLowStockCount(0);
                r.setTodayIncoming(0);
                r.setUsedVolumeM3(BigDecimal.ZERO);
                r.setCapacityPct(0);
            }
            return r;
        });
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouse(UUID id) {
        warehouseAccessService.requireCanViewWarehouseOperations(id);
        Warehouse w = warehouseRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(id)));
        return WarehouseResponse.fromEntity(w);
    }

    @Transactional
    public WarehouseResponse createWarehouse(WarehouseCreateRequest dto) {
        Warehouse w = new Warehouse();
        w.setName(dto.getName().trim());
        w.setLocation(trimToNull(dto.getLocation()));
        w.setAddress(trimToNull(dto.getAddress()));
        w.setGeoZoneId(Optional.ofNullable(dto.getGeoZoneId()).filter(s -> !s.isBlank()).map(UUID::fromString).orElse(null));
        w.setActive(dto.getActive() == null || dto.getActive());
        w.setCapacity(dto.getCapacity());
        Warehouse saved = warehouseRepository.save(w);
        WarehouseResponse response = WarehouseResponse.fromEntity(saved);
        realtimeEventPublisher.publishMutation("POST", "/api/warehouses");
        return response;
    }

    @Transactional
    public WarehouseResponse updateWarehouse(UUID warehouseId, WarehouseCreateRequest dto) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        Warehouse w = warehouseRepository.findDetailById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));

        if (dto.getName() != null) {
            String name = dto.getName().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("name must not be blank when provided");
            }
            w.setName(name);
        }
        if (dto.getLocation() != null) {
            w.setLocation(trimToNull(dto.getLocation()));
        }
        if (dto.getAddress() != null) {
            w.setAddress(trimToNull(dto.getAddress()));
        }
        if (dto.getGeoZoneId() != null) {
            w.setGeoZoneId(dto.getGeoZoneId().isBlank() ? null : UUID.fromString(dto.getGeoZoneId()));
        }
        if (dto.getActive() != null) {
            w.setActive(dto.getActive());
        }
        if (dto.getCapacity() != null) {
            w.setCapacity(dto.getCapacity());
        }
        WarehouseResponse response = WarehouseResponse.fromEntity(warehouseRepository.save(w));
        realtimeEventPublisher.publishMutation("PUT", "/api/warehouses/" + warehouseId);
        return response;
    }

    @Transactional
    public void deleteWarehouse(UUID warehouseId) {
        Warehouse w = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));

        if (warehouseStockRepository.existsByWarehouse_Id(warehouseId)
                || purchaseRepository.existsByWarehouse_Id(warehouseId)) {
            throw new ResourceConflictException("Warehouse has related stock or purchases and cannot be deleted");
        }
        warehouseRepository.delete(w);
        geoServiceClient.deleteZoneDataByWarehouseId(warehouseId);
        realtimeEventPublisher.publishMutation("DELETE", "/api/warehouses/" + warehouseId);
    }

    @Transactional
    public void forceDeleteWarehouse(UUID warehouseId) {
        Warehouse w = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));
        stockLotRepository.deleteByWarehouseId(warehouseId);
        stockMovementRepository.deleteByWarehouseId(warehouseId);
        stockReturnRepository.deleteByWarehouseId(warehouseId);
        inventorySessionRepository.deleteItemsByWarehouseId(warehouseId);
        inventorySessionRepository.deleteByWarehouseId(warehouseId);
        warehouseAdminRepository.deleteByWarehouse_Id(warehouseId);
        warehouseStockRepository.deleteByWarehouse_Id(warehouseId);
        purchaseRepository.deleteByWarehouse_Id(warehouseId);
        auditLogRepository.deleteByWarehouseId(warehouseId);
        warehouseRepository.delete(w);
        geoServiceClient.deleteZoneDataByWarehouseId(warehouseId);
        realtimeEventPublisher.publishMutation("DELETE", "/api/warehouses/" + warehouseId);
    }

    @Transactional
    public WarehouseResponse setWarehouseAdmins(UUID warehouseId, WarehouseAdminsRequest body) {
        Warehouse w = warehouseRepository.findDetailById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));

        List<UUID> uniqueIds = body.getAdminUserIds() == null
                ? List.of()
                : body.getAdminUserIds().stream().distinct().toList();

        for (UUID userId : uniqueIds) {
            if (userId == null) {
                throw new IllegalArgumentException("adminUserIds must not contain null");
            }
        }

        List<UUID> alreadyAssigned = warehouseAdminRepository.findAllDistinctUserIds().stream()
                .filter(id -> !uniqueIds.contains(id))
                .filter(id -> w.getAdminAssignments().stream().noneMatch(a -> a.getUserId().equals(id)))
                .toList();

        List<UUID> currentIds = w.getAdminAssignments().stream()
                .map(WarehouseAdmin::getUserId)
                .toList();

        for (UUID proposedId : uniqueIds) {
            if (!currentIds.contains(proposedId) && alreadyAssigned.contains(proposedId)) {
                throw new IllegalArgumentException(
                        "User " + proposedId + " is already assigned as admin to another warehouse"
                );
            }
        }

        w.getAdminAssignments().clear();
        for (UUID userId : new LinkedHashSet<>(uniqueIds)) {
            WarehouseAdmin link = new WarehouseAdmin();
            link.setWarehouse(w);
            link.setUserId(userId);
            w.getAdminAssignments().add(link);
        }
        return WarehouseResponse.fromEntity(warehouseRepository.save(w));
    }

    @Transactional(readOnly = true)
    public List<UUID> getAllAssignedAdminUserIds() {
        return warehouseAdminRepository.findAllDistinctUserIds();
    }

    @Transactional(readOnly = true)
    public List<UUID> listWarehouseAdmins(UUID warehouseId) {
        Warehouse w = warehouseRepository.findDetailById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));
        if (w.getAdminAssignments() == null) {
            return List.of();
        }
        return w.getAdminAssignments().stream()
                .map(WarehouseAdmin::getUserId)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    @Transactional
    public WarehouseResponse removeWarehouseAdmin(UUID warehouseId, UUID userId) {
        Warehouse w = warehouseRepository.findDetailById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));

        boolean removed = w.getAdminAssignments().removeIf(link -> userId.equals(link.getUserId()));
        if (!removed) {
            throw new ResourceNotFoundException(
                    "Warehouse admin assignment",
                    "warehouseId=" + warehouseId + ", userId=" + userId
            );
        }
        return WarehouseResponse.fromEntity(warehouseRepository.save(w));
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
