package mexa.club.warehouseproject.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseUpdateRequest {

    private String invoiceNumber;
    private LocalDateTime purchaseDate;
}
