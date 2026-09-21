package mexa.club.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private UUID id;
    private String name;
    private String description;
    private Map<String, String> nameTranslations;
    private Map<String, String> descriptionTranslations;
    private UUID parentId;
    private String parentName;
    private String imageUrl;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
