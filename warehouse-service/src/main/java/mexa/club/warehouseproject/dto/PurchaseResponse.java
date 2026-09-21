package mexa.club.warehouseproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.warehouseproject.entity.Purchase;
import mexa.club.warehouseproject.entity.PurchaseItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {

    private UUID id;
    private UUID warehouseId;
    private LocalDateTime purchaseDate;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private List<PurchaseItemResponse> items;

    public static PurchaseResponse fromEntity(Purchase p) {
        List<PurchaseItemResponse> lines = p.getItems() == null
                ? List.of()
                : p.getItems().stream()
                .sorted(Comparator.comparing(PurchaseItem::getId, Comparator.nullsLast(UUID::compareTo)))
                .map(PurchaseItemResponse::fromEntity)
                .toList();
        return PurchaseResponse.builder()
                .id(p.getId())
                .warehouseId(p.getWarehouse() != null ? p.getWarehouse().getId() : null)
                .purchaseDate(p.getPurchaseDate())
                .invoiceNumber(p.getInvoiceNumber())
                .totalAmount(p.getTotalAmount())
                .items(lines)
                .build();
    }
}
