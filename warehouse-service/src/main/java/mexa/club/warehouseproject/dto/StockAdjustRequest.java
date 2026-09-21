package mexa.club.warehouseproject.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class StockAdjustRequest {

    @NotNull
    private UUID productId;

    /** Musbat — kirim; manfiy — chiqim (qoldiq yetarli bo'lishi kerak). */
    @NotNull
    private BigDecimal quantityDelta;

    private String reason;
}
