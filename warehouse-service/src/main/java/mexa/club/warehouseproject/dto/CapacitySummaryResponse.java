package mexa.club.warehouseproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacitySummaryResponse {

    /** Umumiy ombor sig'imi (m³). */
    private BigDecimal capacityM3;

    /** Barcha stok qatorlari bo'yicha band qilingan hajm (m³). */
    private BigDecimal usedVolumeM3;

    /** Foydalaniladigan sig'im foizi (0..100). */
    private int capacityPct;

    /** Har bir mahsulotning hajm tafsiloti. */
    private List<CapacityItemResponse> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapacityItemResponse {
        private UUID productId;
        private String productName;
        private BigDecimal quantity;
        private double length;
        private double width;
        private double height;
        /** Bitta donaning hajmi (m³). */
        private BigDecimal unitVolumeM3;
        /** Umuniy hajm = quantity × unitVolumeM3 (m³). */
        private BigDecimal volumeM3;
    }
}