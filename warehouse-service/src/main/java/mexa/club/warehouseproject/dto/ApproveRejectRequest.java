package mexa.club.warehouseproject.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApproveRejectRequest {
    @Size(max = 1000)
    private String note;
}
