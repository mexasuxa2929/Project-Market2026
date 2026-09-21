package mexa.club.warehouseproject.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NamedDescriptionRequest {

    @NotBlank
    private String name;

    private String description;
}
