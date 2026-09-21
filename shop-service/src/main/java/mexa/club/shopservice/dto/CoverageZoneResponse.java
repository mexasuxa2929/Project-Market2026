package mexa.club.shopservice.dto;

import java.util.UUID;

/** Mobil xarita uchun xizmat hududi (slim). */
public record CoverageZoneResponse(
        UUID id,
        String name,
        String polygon,
        String color
) {
}
