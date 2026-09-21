package mexa.club.warehouseproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mexa.club.warehouseproject.dto.PurchaseCreateRequest;
import mexa.club.warehouseproject.dto.PurchaseItemRequest;
import mexa.club.warehouseproject.dto.PurchaseResponse;
import mexa.club.warehouseproject.repository.StockLotRepository;
import mexa.club.warehouseproject.service.PurchaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Internal service-to-service endpoint: order-service buyurtmada zaxira
 * yetmaganda avtomatik zakaz (purchase) yaratadi. X-Internal-Secret bilan
 * himoyalangan (JWT emas) — mijoz tokeni STOCK_MANAGE talab qilgani uchun
 * bevosita purchase API ni chaqira olmaydi.
 *
 * Purchase darhol kirim qiladi (PurchaseService.createPurchase), shuning
 * uchun yetmagan qism omborga tushgach orderning to'liq salesOut'i o'tadi.
 * invoiceNumber "AUTO-{orderNumber}" — kuzatuv uchun.
 */
@Tag(name = "Internal — Auto Purchase", description = "Internal service-to-service endpoint for automatic replenishment purchases when order stock is insufficient. Protected by X-Internal-Secret header, not by JWT.")
@RestController
@RequestMapping("/internal/warehouses/{warehouseId}/auto-purchase")
public class InternalAutoPurchaseController {

    private final PurchaseService purchaseService;
    private final StockLotRepository stockLotRepository;
    private final String internalSecret;

    public InternalAutoPurchaseController(
            PurchaseService purchaseService,
            StockLotRepository stockLotRepository,
            @Value("${app.internal-secret}") String internalSecret
    ) {
        this.purchaseService = purchaseService;
        this.stockLotRepository = stockLotRepository;
        this.internalSecret = internalSecret;
    }

    public record AutoPurchaseItem(UUID productId, BigDecimal quantity) {}

    public record AutoPurchaseRequest(String orderNumber, List<AutoPurchaseItem> items) {}

    public record AutoPurchaseResponse(UUID purchaseId, String invoiceNumber, int itemCount) {}

    @Operation(
            summary = "Create automatic replenishment purchase",
            description = "Creates a purchase (instant stock receipt) for the missing quantities of an order. Unit price is taken from the last received lot, or 0 if never purchased (staff corrects it later)."
    )
    @PostMapping
    public AutoPurchaseResponse autoPurchase(
            @Parameter(description = "UUID of the warehouse", required = true) @PathVariable UUID warehouseId,
            @Parameter(description = "Shared internal secret", required = true) @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestBody AutoPurchaseRequest request
    ) {
        if (secret == null || !secret.equals(internalSecret)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid internal secret");
        }
        if (request == null || request.orderNumber() == null || request.orderNumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderNumber is required");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items must not be empty");
        }

        PurchaseCreateRequest dto = new PurchaseCreateRequest();
        dto.setInvoiceNumber("AUTO-" + request.orderNumber().trim());
        dto.setPurchaseDate(LocalDateTime.now());
        List<PurchaseItemRequest> lines = new ArrayList<>();
        for (AutoPurchaseItem item : request.items()) {
            if (item.productId() == null || item.quantity() == null
                    || item.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each item needs productId and quantity > 0");
            }
            PurchaseItemRequest line = new PurchaseItemRequest();
            line.setProductId(item.productId());
            line.setQuantity(item.quantity());
            // Tannarx: shu ombordagi oxirgi kirim narxi; hech qachon kelmagan bo'lsa 0
            // (xodim keyin to'g'rilaydi — invoice AUTO- prefiksi bilan ajralib turadi)
            BigDecimal unitPrice = stockLotRepository
                    .findTopByWarehouseIdAndProductIdOrderByReceivedDateDesc(warehouseId, item.productId())
                    .map(lot -> lot.getUnitCost() != null ? lot.getUnitCost() : BigDecimal.ZERO)
                    .orElse(BigDecimal.ZERO);
            line.setUnitPrice(unitPrice);
            lines.add(line);
        }
        dto.setItems(lines);

        PurchaseResponse created = purchaseService.createPurchase(warehouseId, dto);
        return new AutoPurchaseResponse(created.getId(), created.getInvoiceNumber(), lines.size());
    }
}
