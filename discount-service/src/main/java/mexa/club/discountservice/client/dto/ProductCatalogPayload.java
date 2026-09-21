package mexa.club.discountservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductCatalogPayload(
        UUID id,
        UUID categoryId
) {}
