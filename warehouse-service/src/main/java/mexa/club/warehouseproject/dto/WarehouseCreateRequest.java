package mexa.club.warehouseproject.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WarehouseCreateRequest {

    @NotBlank
    private String name;

    private String location;
    private String address;
    private String geoZoneId;
    private Boolean active;
    private Integer capacity;
}
