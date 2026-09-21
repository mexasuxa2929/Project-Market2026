package mexa.club.warehouseproject.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import mexa.club.warehouseproject.entity.MovementType;
import mexa.club.warehouseproject.entity.Purchase;
import mexa.club.warehouseproject.entity.PurchaseItem;
import mexa.club.warehouseproject.entity.StockMovement;
import mexa.club.warehouseproject.entity.Warehouse;
import mexa.club.warehouseproject.entity.WarehouseStock;
import mexa.club.warehouseproject.exception.ResourceNotFoundException;
import mexa.club.warehouseproject.repository.PurchaseRepository;
import mexa.club.warehouseproject.repository.StockMovementRepository;
import mexa.club.warehouseproject.repository.WarehouseRepository;
import mexa.club.warehouseproject.repository.WarehouseStockRepository;
import mexa.club.warehouseproject.security.WarehouseAccessService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WarehouseReportService {

    private final WarehouseStockRepository warehouseStockRepository;
    private final PurchaseRepository purchaseRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductReferenceService productReferenceService;
    private final WarehouseAccessService warehouseAccessService;

    public WarehouseReportService(
            WarehouseStockRepository warehouseStockRepository,
            PurchaseRepository purchaseRepository,
            StockMovementRepository stockMovementRepository,
            WarehouseRepository warehouseRepository,
            ProductReferenceService productReferenceService,
            WarehouseAccessService warehouseAccessService
    ) {
        this.warehouseStockRepository = warehouseStockRepository;
        this.purchaseRepository = purchaseRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.warehouseRepository = warehouseRepository;
        this.productReferenceService = productReferenceService;
        this.warehouseAccessService = warehouseAccessService;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> stockReport(UUID warehouseId, String format) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        List<WarehouseStock> lines = warehouseStockRepository.findAllByWarehouse_Id(warehouseId);
        if ("csv".equalsIgnoreCase(format)) {
            byte[] csv = buildStockCsv(lines).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return attachment(csv, "stock-" + LocalDate.now() + ".csv", "text/csv");
        }
        byte[] bytes = buildStockXlsx(lines);
        return attachment(bytes, "stock-" + LocalDate.now() + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> purchasesReport(UUID warehouseId, LocalDate dateFrom, LocalDate dateTo, String format) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        List<Purchase> purchases = purchaseRepository.findByWarehouseId(warehouseId, Pageable.unpaged()).getContent();
        if ("csv".equalsIgnoreCase(format)) {
            byte[] csv = buildPurchasesCsv(purchases, dateFrom, dateTo).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return attachment(csv, "purchases-" + LocalDate.now() + ".csv", "text/csv");
        }
        byte[] bytes = buildPurchasesXlsx(purchases, dateFrom, dateTo);
        return attachment(bytes, "purchases-" + LocalDate.now() + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> movementsReport(UUID warehouseId, LocalDateTime dateFrom, LocalDateTime dateTo, MovementType movementType, String format) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        List<StockMovement> rows = stockMovementRepository.findAll(
                stockMovementRepository.filterSpec(warehouseId, null, movementType, dateFrom, dateTo),
                Pageable.unpaged()).getContent();
        if ("csv".equalsIgnoreCase(format)) {
            byte[] csv = buildMovementsCsv(rows).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return attachment(csv, "movements-" + LocalDate.now() + ".csv", "text/csv");
        }
        byte[] bytes = buildMovementsXlsx(rows);
        return attachment(bytes, "movements-" + LocalDate.now() + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> purchasePdf(UUID warehouseId, UUID purchaseId) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        Purchase purchase = purchaseRepository.findByIdAndWarehouseId(purchaseId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", String.valueOf(purchaseId)));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));
        byte[] bytes = buildPurchasePdf(warehouse, purchase);
        return attachment(bytes, "purchase-" + purchaseId + ".pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    private byte[] buildStockXlsx(List<WarehouseStock> lines) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("stock");
            Row h = sheet.createRow(0);
            String[] headers = {"№","Mahsulot nomi","Barcode","Kategoriya","Brend","Tizim soni","Rezerv","Mavjud","Minimal norm","Holat"};
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);
            int idx = 1;
            for (WarehouseStock s : lines) {
                var variant = productReferenceService.findVariant(s.getProductId()).orElse(null);
                BigDecimal quantity = s.getQuantity() != null ? s.getQuantity() : BigDecimal.ZERO;
                BigDecimal reserved = s.getReservedQuantity() != null ? s.getReservedQuantity() : BigDecimal.ZERO;
                BigDecimal available = quantity.subtract(reserved);
                int minStock = resolveMinStock(s);
                Row row = sheet.createRow(idx);
                row.createCell(0).setCellValue(idx++);
                row.createCell(1).setCellValue(variant != null ? variant.name() : "Unknown");
                row.createCell(2).setCellValue("");
                row.createCell(3).setCellValue("");
                row.createCell(4).setCellValue("");
                row.createCell(5).setCellValue(quantity.toPlainString());
                row.createCell(6).setCellValue(reserved.toPlainString());
                row.createCell(7).setCellValue(available.toPlainString());
                row.createCell(8).setCellValue(minStock);
                row.createCell(9).setCellValue(available.compareTo(BigDecimal.valueOf(minStock)) < 0 ? "PAST" : "OK");
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build stock report", ex);
        }
    }

    private String buildStockCsv(List<WarehouseStock> lines) {
        StringBuilder sb = new StringBuilder("№,Mahsulot nomi,Tizim soni,Rezerv,Mavjud,Minimal norm,Holat\n");
        int idx = 1;
        for (WarehouseStock s : lines) {
            var variant = productReferenceService.findVariant(s.getProductId()).orElse(null);
            BigDecimal quantity = s.getQuantity() != null ? s.getQuantity() : BigDecimal.ZERO;
            BigDecimal reserved = s.getReservedQuantity() != null ? s.getReservedQuantity() : BigDecimal.ZERO;
            BigDecimal available = quantity.subtract(reserved);
            int minStock = resolveMinStock(s);
            sb.append(idx++).append(',')
                    .append(variant != null ? safeCsv(variant.name()) : "Unknown").append(',')
                    .append(quantity).append(',')
                    .append(reserved).append(',')
                    .append(available).append(',')
                    .append(minStock).append(',')
                    .append(available.compareTo(BigDecimal.valueOf(minStock)) < 0 ? "PAST" : "OK")
                    .append('\n');
        }
        return sb.toString();
    }

    private byte[] buildPurchasesXlsx(List<Purchase> purchases, LocalDate dateFrom, LocalDate dateTo) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("purchases");
            Row h = sheet.createRow(0);
            String[] headers = {"Sana","Faktura №","Mahsulot","Miqdor","Narx","Jami"};
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);
            int r = 1;
            for (Purchase p : purchases) {
                if (dateFrom != null && p.getPurchaseDate().toLocalDate().isBefore(dateFrom)) continue;
                if (dateTo != null && p.getPurchaseDate().toLocalDate().isAfter(dateTo)) continue;
                for (PurchaseItem item : p.getItems()) {
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(String.valueOf(p.getPurchaseDate()));
                    row.createCell(1).setCellValue(p.getInvoiceNumber() != null ? p.getInvoiceNumber() : "");
                    row.createCell(2).setCellValue(productReferenceService.findVariant(item.getProductId()).map(ProductReferenceService.VariantSnapshot::name).orElse("Unknown"));
                    row.createCell(3).setCellValue(item.getQuantity() != null ? item.getQuantity().toPlainString() : "0");
                    row.createCell(4).setCellValue(item.getUnitPrice() != null ? item.getUnitPrice().toPlainString() : "0");
                    row.createCell(5).setCellValue(item.getTotalPrice() != null ? item.getTotalPrice().toPlainString() : "0");
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build purchases report", ex);
        }
    }

    private String buildPurchasesCsv(List<Purchase> purchases, LocalDate dateFrom, LocalDate dateTo) {
        StringBuilder sb = new StringBuilder("Sana,Faktura №,Mahsulot,Miqdor,Narx,Jami\n");
        for (Purchase p : purchases) {
            if (dateFrom != null && p.getPurchaseDate().toLocalDate().isBefore(dateFrom)) continue;
            if (dateTo != null && p.getPurchaseDate().toLocalDate().isAfter(dateTo)) continue;
            for (PurchaseItem item : p.getItems()) {
                sb.append(p.getPurchaseDate()).append(',')
                        .append(safeCsv(p.getInvoiceNumber())).append(',')
                        .append(safeCsv(productReferenceService.findVariant(item.getProductId()).map(ProductReferenceService.VariantSnapshot::name).orElse("Unknown"))).append(',')
                        .append(item.getQuantity()).append(',')
                        .append(item.getUnitPrice()).append(',')
                        .append(item.getTotalPrice()).append('\n');
            }
        }
        return sb.toString();
    }

    private byte[] buildMovementsXlsx(List<StockMovement> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("movements");
            Row h = sheet.createRow(0);
            String[] headers = {"Sana","Turi","Mahsulot","Miqdor","Tannarx","Umumiy tannarx","ReferenceType","ReferenceId","Sabab","Kim"};
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);
            int r = 1;
            for (StockMovement m : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(String.valueOf(m.getCreatedAt()));
                row.createCell(1).setCellValue(String.valueOf(m.getMovementType()));
                row.createCell(2).setCellValue(productReferenceService.findVariant(m.getProductId()).map(ProductReferenceService.VariantSnapshot::name).orElse("Unknown"));
                row.createCell(3).setCellValue(m.getQuantity() != null ? m.getQuantity().toPlainString() : "0");
                row.createCell(4).setCellValue(m.getUnitCost() != null ? m.getUnitCost().toPlainString() : "");
                row.createCell(5).setCellValue(m.getTotalCost() != null ? m.getTotalCost().toPlainString() : "");
                row.createCell(6).setCellValue(m.getReferenceType() != null ? m.getReferenceType() : "");
                row.createCell(7).setCellValue(m.getReferenceId() != null ? String.valueOf(m.getReferenceId()) : "");
                row.createCell(8).setCellValue(m.getReason() != null ? m.getReason() : "");
                row.createCell(9).setCellValue(m.getActorUsername() != null ? m.getActorUsername() : "");
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build movements report", ex);
        }
    }

    private String buildMovementsCsv(List<StockMovement> rows) {
        StringBuilder sb = new StringBuilder("Sana,Turi,Mahsulot,Miqdor,Tannarx,Umumiy tannarx,ReferenceType,ReferenceId,Sabab,Kim\n");
        for (StockMovement m : rows) {
            sb.append(m.getCreatedAt()).append(',')
                    .append(m.getMovementType()).append(',')
                    .append(safeCsv(productReferenceService.findVariant(m.getProductId()).map(ProductReferenceService.VariantSnapshot::name).orElse("Unknown"))).append(',')
                    .append(m.getQuantity()).append(',')
                    .append(m.getUnitCost() != null ? m.getUnitCost() : "").append(',')
                    .append(m.getTotalCost() != null ? m.getTotalCost() : "").append(',')
                    .append(safeCsv(m.getReferenceType())).append(',')
                    .append(m.getReferenceId() != null ? m.getReferenceId() : "").append(',')
                    .append(safeCsv(m.getReason())).append(',')
                    .append(safeCsv(m.getActorUsername()))
                    .append('\n');
        }
        return sb.toString();
    }

    private byte[] buildPurchasePdf(Warehouse warehouse, Purchase purchase) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("Warehouse: " + warehouse.getName()));
            document.add(new Paragraph("Address: " + (warehouse.getAddress() != null ? warehouse.getAddress() : "")));
            document.add(new Paragraph("Invoice: " + (purchase.getInvoiceNumber() != null ? purchase.getInvoiceNumber() : "")));
            document.add(new Paragraph("Date: " + purchase.getPurchaseDate()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell(new PdfPCell(new Phrase("Mahsulot")));
            table.addCell(new PdfPCell(new Phrase("Miqdor")));
            table.addCell(new PdfPCell(new Phrase("Narx")));
            table.addCell(new PdfPCell(new Phrase("Jami")));
            for (PurchaseItem item : purchase.getItems()) {
                table.addCell(productReferenceService.findVariant(item.getProductId()).map(ProductReferenceService.VariantSnapshot::name).orElse("Unknown"));
                table.addCell(item.getQuantity() != null ? item.getQuantity().toPlainString() : "0");
                table.addCell(item.getUnitPrice() != null ? item.getUnitPrice().toPlainString() : "0");
                table.addCell(item.getTotalPrice() != null ? item.getTotalPrice().toPlainString() : "0");
            }
            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Umumiy summa: " + (purchase.getTotalAmount() != null ? purchase.getTotalAmount().toPlainString() : "0")));
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build purchase pdf", ex);
        }
    }

    private int resolveMinStock(WarehouseStock s) {
        if (s.getMinStockOverride() != null) {
            return s.getMinStockOverride();
        }
        return productReferenceService.findProduct(s.getProductId())
                .map(ProductReferenceService.ProductSnapshot::minStock)
                .orElse(0);
    }

    private static ResponseEntity<byte[]> attachment(byte[] data, String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(data);
    }

    private static String safeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
