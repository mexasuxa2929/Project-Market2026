package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.dto.ProfitReportResponse;
import mexa.club.warehouseproject.entity.MovementType;
import mexa.club.warehouseproject.entity.StockMovement;
import mexa.club.warehouseproject.exception.ResourceNotFoundException;
import mexa.club.warehouseproject.repository.OperatingExpenseRepository;
import mexa.club.warehouseproject.repository.StockMovementRepository;
import mexa.club.warehouseproject.repository.WarehouseRepository;
import mexa.club.warehouseproject.security.WarehouseAccessService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Profit report: revenue is derived from the current unified sale price of each
 * product (product-service {@code basePrice}) times the quantity sold; COGS is
 * the FIFO lot cost snapshot stored on each SALES_OUT movement.
 */
@Service
public class ProfitReportService {

    private final StockMovementRepository stockMovementRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductReferenceService productReferenceService;
    private final WarehouseAccessService warehouseAccessService;
    private final OperatingExpenseRepository operatingExpenseRepository;

    public ProfitReportService(
            StockMovementRepository stockMovementRepository,
            WarehouseRepository warehouseRepository,
            ProductReferenceService productReferenceService,
            WarehouseAccessService warehouseAccessService,
            OperatingExpenseRepository operatingExpenseRepository
    ) {
        this.stockMovementRepository = stockMovementRepository;
        this.warehouseRepository = warehouseRepository;
        this.productReferenceService = productReferenceService;
        this.warehouseAccessService = warehouseAccessService;
        this.operatingExpenseRepository = operatingExpenseRepository;
    }

    @Transactional(readOnly = true)
    public ProfitReportResponse profitReport(UUID warehouseId, LocalDateTime dateFrom, LocalDateTime dateTo) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId));
        }

        List<StockMovement> sales = stockMovementRepository.findAll(
                stockMovementRepository.filterSpec(warehouseId, null, MovementType.SALES_OUT, dateFrom, dateTo),
                Pageable.unpaged()).getContent();
        List<StockMovement> returns = stockMovementRepository.findAll(
                stockMovementRepository.filterSpec(warehouseId, null, MovementType.RETURN_IN, dateFrom, dateTo),
                Pageable.unpaged()).getContent();

        Map<UUID, SoldLine> byProduct = new LinkedHashMap<>();
        for (StockMovement m : sales) {
            SoldLine line = byProduct.computeIfAbsent(m.getProductId(), k -> new SoldLine());
            BigDecimal qty = m.getQuantity() != null ? m.getQuantity() : BigDecimal.ZERO;
            line.soldQuantity = line.soldQuantity.add(qty);
            if (m.getTotalCost() != null) {
                line.cost = line.cost.add(m.getTotalCost());
            }
        }
        for (StockMovement m : returns) {
            SoldLine line = byProduct.computeIfAbsent(m.getProductId(), k -> new SoldLine());
            BigDecimal qty = m.getQuantity() != null ? m.getQuantity() : BigDecimal.ZERO;
            line.returnedQuantity = line.returnedQuantity.add(qty);
            BigDecimal returnedCost = m.getTotalCost() != null
                    ? m.getTotalCost()
                    : (m.getUnitCost() != null
                        ? m.getUnitCost().multiply(qty)
                        : BigDecimal.ZERO);
            line.returnedCost = line.returnedCost.add(returnedCost);
        }

        List<ProfitReportResponse.ProductProfitLine> lines = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (Map.Entry<UUID, SoldLine> e : byProduct.entrySet()) {
            UUID productId = e.getKey();
            SoldLine line = e.getValue();
            BigDecimal netQuantity = line.soldQuantity.subtract(line.returnedQuantity).max(BigDecimal.ZERO);
            BigDecimal netCost = line.cost.subtract(line.returnedCost).max(BigDecimal.ZERO);
            BigDecimal salePrice = productReferenceService.findSalePrice(productId).orElse(BigDecimal.ZERO);
            BigDecimal revenue = netQuantity.multiply(salePrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cost = netCost.setScale(2, RoundingMode.HALF_UP);
            BigDecimal profit = revenue.subtract(cost);
            totalRevenue = totalRevenue.add(revenue);
            totalCost = totalCost.add(cost);

            String name = productReferenceService.findVariant(productId)
                    .map(ProductReferenceService.VariantSnapshot::name)
                    .orElse("Unknown");
            lines.add(ProfitReportResponse.ProductProfitLine.builder()
                    .productId(productId)
                    .productName(name)
                    .soldQuantity(netQuantity)
                    .returnedQuantity(line.returnedQuantity)
                    .returnedCost(line.returnedCost)
                    .salePrice(salePrice)
                    .revenue(revenue)
                    .cost(cost)
                    .profit(profit)
                    .build());
        }

        BigDecimal grossProfit = totalRevenue.subtract(totalCost);
        // Operating expenses are warehouse rent, salary, electricity etc. — separate from COGS
        BigDecimal totalOperatingExpense = BigDecimal.ZERO;
        if (dateFrom != null && dateTo != null) {
            totalOperatingExpense = operatingExpenseRepository.sumByWarehouseAndDate(warehouseId, dateFrom, dateTo);
            if (totalOperatingExpense == null) totalOperatingExpense = BigDecimal.ZERO;
        } else if (dateFrom != null || dateTo != null) {
            LocalDateTime from = dateFrom != null ? dateFrom : LocalDateTime.of(1970,1,1,0,0);
            LocalDateTime to = dateTo != null ? dateTo : LocalDateTime.now();
            totalOperatingExpense = operatingExpenseRepository.sumByWarehouseAndDate(warehouseId, from, to);
            if (totalOperatingExpense == null) totalOperatingExpense = BigDecimal.ZERO;
        }
        BigDecimal operatingProfit = grossProfit.subtract(totalOperatingExpense);

        return ProfitReportResponse.builder()
                .warehouseId(warehouseId)
                .productCount(lines.size())
                .totalRevenue(totalRevenue)
                .totalCost(totalCost)
                .totalProfit(grossProfit)
                .totalOperatingExpense(totalOperatingExpense)
                .operatingProfit(operatingProfit)
                .lines(lines)
                .build();
    }

    private static final class SoldLine {
        private BigDecimal soldQuantity = BigDecimal.ZERO;
        private BigDecimal cost = BigDecimal.ZERO;
        private BigDecimal returnedQuantity = BigDecimal.ZERO;
        private BigDecimal returnedCost = BigDecimal.ZERO;
    }
}
