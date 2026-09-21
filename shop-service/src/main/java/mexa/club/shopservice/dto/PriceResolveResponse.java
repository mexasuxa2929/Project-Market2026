package mexa.club.shopservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mirrors product-service's PriceResolveResponse — miqdorga bog'liq yagona narx.
 * 2+ liniya bo'lsa liniya narxi (muddatli chegirma ishlamaydi), aks holda chegirmali asosiy narx.
 */
public record PriceResolveResponse(
        UUID productId,
        int qty,
        BigDecimal unitPrice,
        Integer discountPercent,
        boolean tierApplied,
        int lineCount
) {}