package mexa.club.shopservice.dto;

/** Nuqta xizmat hududiga kiradimi (zona nomi bilan). */
public record CoverageCheckResponse(
        boolean served,
        String zoneName
) {
}
