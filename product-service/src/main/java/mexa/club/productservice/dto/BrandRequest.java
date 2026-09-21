package mexa.club.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record BrandRequest(
        @NotBlank(message = "Brend nomi majburiy")
        @Size(max = 150, message = "Brend nomi 150 ta belgidan oshmasligi kerak")
        String name,

        String description,

        Map<String, String> nameTranslations,
        Map<String, String> descriptionTranslations,

        Boolean active
) {}
