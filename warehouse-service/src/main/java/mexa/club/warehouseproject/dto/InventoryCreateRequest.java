package mexa.club.warehouseproject.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InventoryCreateRequest {
    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;
}
