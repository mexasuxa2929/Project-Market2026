package mexa.club.orderservice.dto;

import java.math.BigDecimal;

/**
 * Checkout preview: order yaratilmasdan oldin server hisoblagan
 * subtotal / chegirma / yetkazish haqi / jami summa (total = subtotal - discount + fee).
 */
public record OrderPreviewResponse(
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        boolean freeDelivery,
        String discountMessage,
        String incentiveMessage
) {}
