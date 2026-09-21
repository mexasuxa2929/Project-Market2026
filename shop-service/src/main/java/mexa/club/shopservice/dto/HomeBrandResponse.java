package mexa.club.shopservice.dto;

import java.util.UUID;

/**
 * Homepage bundle ichidagi brend — faqat ro'yxat uchun kerakli maydonlar.
 * product-service BrandResponse'ning qolgan maydonlari Jackson tomonidan e'tiborsiz qoldiriladi.
 */
public record HomeBrandResponse(
        UUID id,
        String name,
        String logoUrl
) {}
