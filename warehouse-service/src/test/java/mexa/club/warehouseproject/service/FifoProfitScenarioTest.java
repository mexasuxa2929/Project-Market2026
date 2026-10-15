package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.entity.StockLot;
import mexa.club.warehouseproject.entity.StockMovement;
import mexa.club.warehouseproject.repository.OperatingExpenseRepository;
import mexa.club.warehouseproject.repository.StockLotRepository;
import mexa.club.warehouseproject.repository.StockMovementRepository;
import mexa.club.warehouseproject.repository.WarehouseRepository;
import mexa.club.warehouseproject.security.WarehouseAccessService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Real holat testi: Lot-1 = 4 dona × 20 000, Lot-2 = 5 dona × 25 000 (jami 9).
 * Haqiqiy {@link StockLotService#consumeFifo} va haqiqiy
 * {@link ProfitReportService#profitReport} ishlatiladi — faqat DB repositorylar
 * mock (real entity obyektlar bilan).
 */
public class FifoProfitScenarioTest {

    private static final BigDecimal LOT1_QTY = new BigDecimal("4");
    private static final BigDecimal LOT1_COST = new BigDecimal("20000");
    private static final BigDecimal LOT2_QTY = new BigDecimal("5");
    private static final BigDecimal LOT2_COST = new BigDecimal("25000");
    private static final BigDecimal SALE_PRICE = new BigDecimal("30000");

    /** Ikkita real lot — FIFO tartibda (eski birinchi), xuddi DB so'rovi kabi. */
    private List<StockLot> freshLots(UUID wh, UUID product) {
        StockLot lot1 = new StockLot();
        lot1.setWarehouseId(wh);
        lot1.setProductId(product);
        lot1.setQuantity(LOT1_QTY);
        lot1.setUnitCost(LOT1_COST);
        lot1.setReceivedDate(LocalDateTime.now().minusDays(2));
        StockLot lot2 = new StockLot();
        lot2.setWarehouseId(wh);
        lot2.setProductId(product);
        lot2.setQuantity(LOT2_QTY);
        lot2.setUnitCost(LOT2_COST);
        lot2.setReceivedDate(LocalDateTime.now().minusDays(1));
        return List.of(lot1, lot2);
    }

    private StockLotRepository lotRepoReturning(List<StockLot> lots) {
        StockLotRepository repo = mock(StockLotRepository.class);
        when(repo.findLotsForUpdate(any(), any())).thenReturn(lots);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        return repo;
    }

    /** salesOut dagi kabi real harakat obyektini qurish. */
    private StockMovement saleMovement(UUID product, BigDecimal qty,
                                       StockLotService.ConsumptionResult consumption) {
        StockMovement m = new StockMovement();
        m.setProductId(product);
        m.setQuantity(qty);
        m.setUnitCost(consumption.unitCost());
        m.setTotalCost(consumption.totalCost());
        m.setUnitPrice(SALE_PRICE);
        m.setTotalRevenue(SALE_PRICE.multiply(qty).setScale(2, java.math.RoundingMode.HALF_UP));
        return m;
    }

    private ProfitReportService profitService(StockMovement sale, BigDecimal currentBasePrice) {
        StockMovementRepository movementRepo = mock(StockMovementRepository.class);
        WarehouseRepository warehouseRepo = mock(WarehouseRepository.class);
        ProductReferenceService productRef = mock(ProductReferenceService.class);
        WarehouseAccessService access = mock(WarehouseAccessService.class);
        OperatingExpenseRepository expenseRepo = mock(OperatingExpenseRepository.class);
        when(warehouseRepo.existsById(any())).thenReturn(true);
        when(expenseRepo.sumByWarehouseAndDate(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        doReturn(new PageImpl<>(List.of(sale)))
                .doReturn(new PageImpl<>(List.of()))
                .when(movementRepo).findAll(Mockito.<Specification<StockMovement>>any(), any(Pageable.class));
        when(productRef.findSalePrice(sale.getProductId())).thenReturn(Optional.of(currentBasePrice));
        when(productRef.findVariant(sale.getProductId())).thenReturn(Optional.of(
                new ProductReferenceService.VariantSnapshot(sale.getProductId(), "Test", "SKU")));
        return new ProfitReportService(movementRepo, warehouseRepo, productRef, access, expenseRepo);
    }

    @Test
    void testFullSale9Units_fifoCost205000() {
        UUID wh = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        List<StockLot> lots = freshLots(wh, product);
        StockLotService lotService = new StockLotService(lotRepoReturning(lots));

        // 9 donaning hammasini sotish — 30 000 dan
        StockLotService.ConsumptionResult r = lotService.consumeFifo(wh, product, new BigDecimal("9"));

        // COGS: 4×20000 + 5×25000 = 205 000 (o'rtacha ~22777 emas, FIFO yig'indi)
        assertEquals(0, new BigDecimal("205000.00").compareTo(r.totalCost()));
        assertEquals(0, BigDecimal.ZERO.compareTo(lots.get(0).getQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(lots.get(1).getQuantity()));

        var report = profitService(saleMovement(product, new BigDecimal("9"), r), SALE_PRICE)
                .profitReport(wh, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        assertEquals(0, new BigDecimal("270000.00").compareTo(report.getTotalRevenue()));
        assertEquals(0, new BigDecimal("205000.00").compareTo(report.getTotalCost()));
        assertEquals(0, new BigDecimal("65000.00").compareTo(report.getTotalProfit()));
    }

    @Test
    void testPartialSale6Units_cheapLotFirst() {
        UUID wh = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        List<StockLot> lots = freshLots(wh, product);
        StockLotService lotService = new StockLotService(lotRepoReturning(lots));

        StockLotService.ConsumptionResult r = lotService.consumeFifo(wh, product, new BigDecimal("6"));

        // COGS: 4×20000 + 2×25000 = 130 000; qoldiq Lot-2 da 3 dona
        assertEquals(0, new BigDecimal("130000.00").compareTo(r.totalCost()));
        assertEquals(0, BigDecimal.ZERO.compareTo(lots.get(0).getQuantity()));
        assertEquals(0, new BigDecimal("3").compareTo(lots.get(1).getQuantity()));

        var report = profitService(saleMovement(product, new BigDecimal("6"), r), SALE_PRICE)
                .profitReport(wh, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        assertEquals(0, new BigDecimal("180000.00").compareTo(report.getTotalRevenue()));
        assertEquals(0, new BigDecimal("130000.00").compareTo(report.getTotalCost()));
        assertEquals(0, new BigDecimal("50000.00").compareTo(report.getTotalProfit()));
    }

    @Test
    void testFirst4UnitsComeFromCheapLotOnly() {
        UUID wh = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        List<StockLot> lots = freshLots(wh, product);
        StockLotService lotService = new StockLotService(lotRepoReturning(lots));

        // Faqat 4 dona sotilsa — hammasi arzon Lot-1 dan chiqishi kerak
        StockLotService.ConsumptionResult r = lotService.consumeFifo(wh, product, new BigDecimal("4"));

        assertEquals(0, new BigDecimal("80000.00").compareTo(r.totalCost()));
        assertEquals(0, BigDecimal.ZERO.compareTo(lots.get(0).getQuantity()));
        assertEquals(0, new BigDecimal("5").compareTo(lots.get(1).getQuantity()));
    }

    @Test
    void testLaterPriceChangeDoesNotRewriteHistory() {
        UUID wh = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        List<StockLot> lots = freshLots(wh, product);
        StockLotService lotService = new StockLotService(lotRepoReturning(lots));

        StockLotService.ConsumptionResult r = lotService.consumeFifo(wh, product, new BigDecimal("9"));
        StockMovement sale = saleMovement(product, new BigDecimal("9"), r);

        // Sotuvdan keyin narx 30 000 → 45 000 ga ko'tarildi: tarix o'zgarmasligi kerak
        var report = profitService(sale, new BigDecimal("45000"))
                .profitReport(wh, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        assertEquals(0, new BigDecimal("270000.00").compareTo(report.getTotalRevenue()));
        assertEquals(0, new BigDecimal("65000.00").compareTo(report.getTotalProfit()));
    }
}
