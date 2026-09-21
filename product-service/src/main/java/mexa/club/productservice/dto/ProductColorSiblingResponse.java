package mexa.club.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Bir guruhdagi (group_id) boshqa rangdagi mahsulot haqida qisqa ma'lumot.
 * Variantlar olib tashlangach, har bir rang mustaqil Product hisoblanadi —
 * bu DTO faqat shu ranglarni bir-biriga bog'lab ko'rsatish uchun ishlatiladi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductColorSiblingResponse {
    private UUID id;
    private String color;
    private String colorCode;
    private String barcode;
    private String imageUrl;
    private boolean active;
    private boolean isCurrent;
}
