package mexa.club.warehouseproject.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class StockBatchRequest {
    @NotEmpty
    private List<UUID> productIds;
}
