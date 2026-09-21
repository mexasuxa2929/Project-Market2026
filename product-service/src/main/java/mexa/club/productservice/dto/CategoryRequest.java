package mexa.club.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Kategoriya nomi majburiy")
    @Size(max = 150, message = "Kategoriya nomi 150 ta belgidan oshmasligi kerak")
    private String name;

    private String description;

    private Map<String, String> nameTranslations;
    private Map<String, String> descriptionTranslations;

    private UUID parentId;

    private String imageUrl;

    private Boolean active;
}
