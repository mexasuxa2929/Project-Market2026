package mexa.club.shopservice.dto;

import java.util.UUID;

/**
 * Homepage bundle ichidagi kategoriya — faqat ro'yxat uchun kerakli maydonlar.
 * product-service CategoryResponse'ning qolgan maydonlari Jackson tomonidan e'tiborsiz qoldiriladi.
 */
public record HomeCategoryResponse(
        UUID id,
        String name,
        String imageUrl,
        UUID parentId
) {}
