package mexa.club.orderservice.client.payload;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mirrors product-service's PriceResolveResponse — miqdorga bog'liq yagona narx resolve natijasi.
 * Qoidalar (product-service tomonda):
 *  - 2+ liniya bo'lsa liniya narxi qaytadi, muddatli chegirma ishlamaydi;
 *  - aks holda asosiy narx ustidan muddatli chegirma qo'llanadi.
 */
public record PriceResolveResponse(
        UUID productId,
        int qty,
        BigDecimal unitPrice,
        Integer discountPercent,
        boolean tierApplied,
        int lineCount
) {}