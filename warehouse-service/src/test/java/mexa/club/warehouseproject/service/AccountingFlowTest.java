package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.entity.StockMovement;
import mexa.club.warehouseproject.repository.OperatingExpenseRepository;
import mexa.club.warehouseproject.repository.StockMovementRepository;
import mexa.club.warehouseproject.repository.WarehouseRepository;
import mexa.club.warehouseproject.security.WarehouseAccessService;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;
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

public class AccountingFlowTest {

    @Test
    void testPurchaseDoesNotAffectProfit() {
        StockMovementRepository movementRepo = mock(StockMovementRepository.class);
        WarehouseRepository warehouseRepo = mock(WarehouseRepository.class);
        ProductReferenceService productRef = mock(ProductReferenceService.class);
        WarehouseAccessService access = mock(WarehouseAccessService.class);
        OperatingExpenseRepository expenseRepo = mock(OperatingExpenseRepository.class);

        when(warehouseRepo.existsById(any())).thenReturn(true);
        doReturn(new PageImpl<>(List.of()))
                .doReturn(new PageImpl<>(List.of()))
                .when(movementRepo).findAll(Mockito.<Specification<StockMovement>>any(), any(Pageable.class));
        when(expenseRepo.sumByWarehouseAndDate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        ProfitReportService service = new ProfitReportService(movementRepo, warehouseRepo, productRef, access, expenseRepo);
        UUID wh = UUID.randomUUID();
        var report = service.profitReport(wh, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        assertEquals(0, report.getTotalRevenue().compareTo(BigDecimal.ZERO));
        assertEquals(0, report.getTotalCost().compareTo(BigDecimal.ZERO));
        assertEquals(0, report.getTotalProfit().compareTo(BigDecimal.ZERO));
        assertEquals(0, report.getTotalOperatingExpense().compareTo(BigDecimal.ZERO));
        assertEquals(0, report.getOperatingProfit().compareTo(BigDecimal.ZERO));
    }

    @Test
    void testWarehouseExpenseDoesNotAffectCOGS() {
        StockMovementRepository movementRepo = mock(StockMovementRepository.class);
        WarehouseRepository warehouseRepo = mock(WarehouseRepository.class);
        ProductReferenceService productRef = mock(ProductReferenceService.class);
        WarehouseAccessService access = mock(WarehouseAccessService.class);
        OperatingExpenseRepository expenseRepo = mock(OperatingExpenseRepository.class);
        when(warehouseRepo.existsById(any())).thenReturn(true);
        doReturn(new PageImpl<>(List.of()))
                .doReturn(new PageImpl<>(List.of()))
                .when(movementRepo).findAll(Mockito.<Specification<StockMovement>>any(), any(Pageable.class));
        when(expenseRepo.sumByWarehouseAndDate(any(), any(), any())).thenReturn(new BigDecimal("500.00"));

        ProfitReportService service = new ProfitReportService(movementRepo, warehouseRepo, productRef, access, expenseRepo);
        UUID wh = UUID.randomUUID();
        var report = service.profitReport(wh, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        assertEquals(0, report.getTotalCost().compareTo(BigDecimal.ZERO));
        assertEquals(0, report.getTotalProfit().compareTo(BigDecimal.ZERO));
        assertEquals(0, new BigDecimal("500.00").compareTo(report.getTotalOperatingExpense()));
        assertEquals(0, new BigDecimal("-500.00").compareTo(report.getOperatingProfit()));
    }

    @Test
    void testPartialSaleProfit() {
        StockMovementRepository movementRepo = mock(StockMovementRepository.class);
        WarehouseRepository warehouseRepo = mock(WarehouseRepository.class);
        ProductReferenceService productRef = mock(ProductReferenceService.class);
        WarehouseAccessService access = mock(WarehouseAccessService.class);
        OperatingExpenseRepository expenseRepo = mock(OperatingExpenseRepository.class);
        when(warehouseRepo.existsById(any())).thenReturn(true);
        when(expenseRepo.sumByWarehouseAndDate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        UUID wh = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        StockMovement sale = new StockMovement();
        sale.setProductId(product);
        sale.setQuantity(new BigDecimal("3"));
        sale.setTotalCost(new BigDecimal("30.00"));
        sale.setUnitCost(new BigDecimal("10.000000"));

        doReturn(new PageImpl<>(List.of(sale)))
                .doReturn(new PageImpl<>(List.of()))
                .when(movementRepo).findAll(Mockito.<Specification<StockMovement>>any(), any(Pageable.class));

        when(productRef.findSalePrice(product)).thenReturn(Optional.of(new BigDecimal("15.00")));
        when(productRef.findVariant(product)).thenReturn(Optional.of(new ProductReferenceService.VariantSnapshot(product, "Test Product", "SKU-1")));

        ProfitReportService service = new ProfitReportService(movementRepo, warehouseRepo, productRef, access, expenseRepo);
        var report = service.profitReport(wh, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        // 3 * 15 = 45 revenue, cost 30, profit 15
        assertEquals(0, new BigDecimal("45.00").compareTo(report.getTotalRevenue()));
        assertEquals(0, new BigDecimal("30.00").compareTo(report.getTotalCost()));
        assertEquals(0, new BigDecimal("15.00").compareTo(report.getTotalProfit()));
    }

    @Test
    void testRecordedSalePriceUsedInsteadOfCurrentBasePrice() {
        StockMovementRepository movementRepo = mock(StockMovementRepository.class);
        WarehouseRepository warehouseRepo = mock(WarehouseRepository.class);
        ProductReferenceService productRef = mock(ProductReferenceService.class);
        WarehouseAccessService access = mock(WarehouseAccessService.class);
        OperatingExpenseRepository expenseRepo = mock(OperatingExpenseRepository.class);
        when(warehouseRepo.existsById(any())).thenReturn(true);
        when(expenseRepo.sumByWarehouseAndDate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        UUID wh = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        // Sotuv paytida narx 20.00 bo'lgan (3 dona → tushum 60), keyin narx 15.00 ga tushgan.
        StockMovement sale = new StockMovement();
        sale.setProductId(product);
        sale.setQuantity(new BigDecimal("3"));
        sale.setTotalCost(new BigDecimal("30.00"));
        sale.setUnitCost(new BigDecimal("10.000000"));
        sale.setUnitPrice(new BigDecimal("20.00"));
        sale.setTotalRevenue(new BigDecimal("60.00"));

        doReturn(new PageImpl<>(List.of(sale)))
                .doReturn(new PageImpl<>(List.of()))
                .when(movementRepo).findAll(Mockito.<Specification<StockMovement>>any(), any(Pageable.class));

        when(productRef.findSalePrice(product)).thenReturn(Optional.of(new BigDecimal("15.00")));
        when(productRef.findVariant(product)).thenReturn(Optional.of(new ProductReferenceService.VariantSnapshot(product, "Test Product", "SKU-1")));

        ProfitReportService service = new ProfitReportService(movementRepo, warehouseRepo, productRef, access, expenseRepo);
        var report = service.profitReport(wh, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        // Tarixiy haqiqiy tushum 60 (joriy 45 emas), foyda 60 - 30 = 30
        assertEquals(0, new BigDecimal("60.00").compareTo(report.getTotalRevenue()));
        assertEquals(0, new BigDecimal("30.00").compareTo(report.getTotalCost()));
        assertEquals(0, new BigDecimal("30.00").compareTo(report.getTotalProfit()));
    }

    @Test
    void testCancelledOrderRevenueFullyReversed() {
        StockMovementRepository movementRepo = mock(StockMovementRepository.class);
        WarehouseRepository warehouseRepo = mock(WarehouseRepository.class);
        ProductReferenceService productRef = mock(ProductReferenceService.class);
        WarehouseAccessService access = mock(WarehouseAccessService.class);
        OperatingExpenseRepository expenseRepo = mock(OperatingExpenseRepository.class);
        when(warehouseRepo.existsById(any())).thenReturn(true);
        when(expenseRepo.sumByWarehouseAndDate(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        UUID wh = UUID.randomUUID();
        UUID product = UUID.randomUUID();
        StockMovement sale = new StockMovement();
        sale.setProductId(product);
        sale.setQuantity(new BigDecimal("2"));
        sale.setTotalCost(new BigDecimal("30.00"));
        sale.setUnitCost(new BigDecimal("15.000000"));
        sale.setUnitPrice(new BigDecimal("20.00"));
        sale.setTotalRevenue(new BigDecimal("40.00"));
        // Bekor qilish asl sotuvdagi narx/tushumni aynan qaytaradi
        StockMovement reversal = new StockMovement();
        reversal.setProductId(product);
        reversal.setQuantity(new BigDecimal("2"));
        reversal.setTotalCost(new BigDecimal("30.00"));
        reversal.setUnitCost(new BigDecimal("15.000000"));
        reversal.setUnitPrice(new BigDecimal("20.00"));
        reversal.setTotalRevenue(new BigDecimal("40.00"));

        doReturn(new PageImpl<>(List.of(sale)))
                .doReturn(new PageImpl<>(List.of(reversal)))
                .when(movementRepo).findAll(Mockito.<Specification<StockMovement>>any(), any(Pageable.class));

        when(productRef.findSalePrice(product)).thenReturn(Optional.of(new BigDecimal("99.00")));
        when(productRef.findVariant(product)).thenReturn(Optional.of(new ProductReferenceService.VariantSnapshot(product, "Test Product", "SKU-1")));

        ProfitReportService service = new ProfitReportService(movementRepo, warehouseRepo, productRef, access, expenseRepo);
        var report = service.profitReport(wh, LocalDateTime.now().minusDays(1), LocalDateTime.now());
        // Bekor qilingan order sof nol beradi — joriy narx (99) hisobga kirmasligi kerak
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(report.getTotalRevenue()));
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(report.getTotalCost()));
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(report.getTotalProfit()));
    }
}
