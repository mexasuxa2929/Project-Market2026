package mexa.club.shopservice.dto;

import java.math.BigDecimal;

public record GeoSearchResult(
        String name,
        BigDecimal lat,
        BigDecimal lng
) {}
