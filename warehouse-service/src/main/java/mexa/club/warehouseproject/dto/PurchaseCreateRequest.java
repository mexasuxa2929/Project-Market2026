package mexa.club.warehouseproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request to create a new purchase.")
public class PurchaseCreateRequest {

    @Schema(description = "Invoice/document number (optional)", example = "INV-2024-001")
    private String invoiceNumber;

    @Schema(description = "Purchase date (optional, defaults to now)", example = "2024-01-15T10:30:00")
    private LocalDateTime purchaseDate;

    @Schema(description = "List of purchased items (minimum 1 item required)")
    @NotEmpty
    @Valid
    private List<PurchaseItemRequest> items;
}
