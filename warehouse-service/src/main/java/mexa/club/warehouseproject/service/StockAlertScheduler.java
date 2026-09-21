package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.client.AuthUserClient;
import mexa.club.warehouseproject.client.NotificationClient;
import mexa.club.warehouseproject.dto.LowStockAlertResponse;
import mexa.club.warehouseproject.entity.Warehouse;
import mexa.club.warehouseproject.repository.WarehouseAdminRepository;
import mexa.club.warehouseproject.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Har kuni kamyob stock (low-stock) bo'yicha ombor adminlariga EMAIL orqali ogohlantirish yuboradi.
 */
@Component
public class StockAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockAlertScheduler.class);

    private final WarehouseRepository warehouseRepository;
    private final WarehouseAdminRepository warehouseAdminRepository;
    private final WarehouseStockService warehouseStockService;
    private final AuthUserClient authUserClient;
    private final NotificationClient notificationClient;

    public StockAlertScheduler(
            WarehouseRepository warehouseRepository,
            WarehouseAdminRepository warehouseAdminRepository,
            WarehouseStockService warehouseStockService,
            AuthUserClient authUserClient,
            NotificationClient notificationClient
    ) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseAdminRepository = warehouseAdminRepository;
        this.warehouseStockService = warehouseStockService;
        this.authUserClient = authUserClient;
        this.notificationClient = notificationClient;
    }

    @Scheduled(cron = "${scheduler.low-stock.cron:0 0 8 * * *}")
    public void sendLowStockAlerts() {
        List<Warehouse> warehouses = warehouseRepository.findAllActive();
        if (warehouses.isEmpty()) {
            return;
        }
        log.info("Low-stock alert scheduled run: checking {} active warehouse(s)", warehouses.size());
        for (Warehouse warehouse : warehouses) {
            try {
                processWarehouse(warehouse);
            } catch (Exception e) {
                log.error("Low-stock alert failed for warehouse {}: {}", warehouse.getId(), e.getMessage(), e);
            }
        }
    }

    private void processWarehouse(Warehouse warehouse) {
        List<LowStockAlertResponse> alerts = warehouseStockService.internalLowStockAlerts(warehouse.getId());
        if (alerts.isEmpty()) {
            return;
        }
        List<UUID> adminUserIds = warehouseAdminRepository.findAdminUserIdsByWarehouseId(warehouse.getId());
        if (adminUserIds.isEmpty()) {
            log.debug("Warehouse {} has no admins; skipping low-stock notifications", warehouse.getId());
            return;
        }
        Map<UUID, String> emails = authUserClient.resolveEmails(adminUserIds);
        if (emails.isEmpty()) {
            return;
        }
        for (LowStockAlertResponse alert : alerts) {
            Map<String, Object> variables = Map.of(
                    "warehouseName", warehouse.getName() != null ? warehouse.getName() : "Noma'lum ombor",
                    "productName", alert.getProductName() != null ? alert.getProductName() : "Noma'lum mahsulot",
                    "quantity", String.valueOf(safeInt(alert.getQuantity())),
                    "minStock", alert.getMinStock()
            );
            for (Map.Entry<UUID, String> entry : emails.entrySet()) {
                notificationClient.sendLowStockAlert(entry.getKey(), entry.getValue(), variables);
            }
        }
        log.info("Warehouse {}: sent {} low-stock alert(s) to {} admin(s)",
                warehouse.getId(), alerts.size(), emails.size());
    }

    private static int safeInt(BigDecimal value) {
        return value != null ? value.intValue() : 0;
    }
}
