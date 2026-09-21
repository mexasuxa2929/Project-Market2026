package mexa.club.productservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductPriceRequest {

    @NotNull
    @JsonAlias("price")
    @Schema(description = "Sotuv narxi (yoki `price` alias)", example = "99990")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal salePrice;

    @Schema(description = "Narx kuchga kirish sanasi (agar yuborilmasa server `now()` ishlatadi)",
            example = "2026-04-25T16:30:00")
    private LocalDateTime effectiveDate;

    @Schema(description = "Narx tugash sanasi (ixtiyoriy)", example = "2026-12-31T23:59:59")
    private LocalDateTime endDate;

    @Schema(description = "Chegirma foizi (0..100). Berilsa, discount_start_date va end_date oralig'ida amal qiladi",
            example = "20")
    @DecimalMin(value = "0", inclusive = true)
    @jakarta.validation.constraints.Max(value = 100, message = "discountPercent must be at most 100")
    private Integer discountPercent;

    @Schema(description = "Chegirma boshlanish sanasi (ixtiyoriy, null = darhol boshlanadi)", example = "2026-08-01T00:00:00")
    private LocalDateTime discountStartDate;

    @Schema(description = "Chegirma tugash sanasi (ixtiyoriy, null = cheksiz). Muddat o'tgach chegirma avtomatik o'chadi",
            example = "2026-08-31T23:59:59")
    private LocalDateTime discountEndDate;

    @Schema(description = "Valyuta (ixtiyoriy)", example = "UZS")
    private String currency;

    @Schema(description = "Narx turi",
            allowableValues = {"RETAIL", "WHOLESALE", "PURCHASE"},
            example = "RETAIL")
    private String priceType;
}

