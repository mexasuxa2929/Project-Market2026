package mexa.club.productservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Miqdorga bog'liq narxni yagona resolve qilish natijasi.
 * Qoidalar:
 *  - 2+ liniya (price_tier) bo'lsa — liniyalar g'alaba qiladi, muddatli chegirma amal qilmaydi;
 *  - 0–1 liniya bo'lsa — asosiy narx (salePrice / 1-liniya narxi) ustidan muddatli chegirma hisoblanadi.
 */
public record PriceResolveResponse(
        @Schema(description = "Mahsulot ID", example = "011860d1-dbe2-4003-9317-feff9a82aead") UUID productId,
        @Schema(description = "So'ralgan miqdor (dona)", example = "6") int qty,
        @Schema(description = "1 dona uchun yakuniy narx (liniya yoki chegirmali narx)", example = "95000.00") BigDecimal unitPrice,
        @Schema(description = "Qo'llanilgan chegirma foizi (chegirma faol bo'lsa liniyalar ishlamaydi)", example = "20") Integer discountPercent,
        @Schema(description = "Narx liniya (tier) orqali topilganmi", example = "true") boolean tierApplied,
        @Schema(description = "Mahsulotdagi liniyalar soni (2+ bo'lsa muddatli chegirma o'chadi)", example = "2") int lineCount
) {}
