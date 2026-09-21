package mexa.club.warehouseproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.warehouseproject.entity.Warehouse;
import mexa.club.warehouseproject.entity.WarehouseAdmin;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponse {

    private UUID id;
    private String name;
    private String location;
    private String address;
    private String geoZoneId;
    private boolean active;
    private Integer capacity;
    private List<UUID> adminUserIds;

    private Integer productCount;
    private Integer lowStockCount;
    private Integer todayIncoming;
    private BigDecimal usedVolumeM3;
    private Integer capacityPct;

    public static WarehouseResponse fromEntity(Warehouse w) {
        List<UUID> ids = w.getAdminAssignments() == null
                ? List.of()
                : w.getAdminAssignments().stream()
                .map(WarehouseAdmin::getUserId)
                .sorted(Comparator.naturalOrder())
                .toList();
        return WarehouseResponse.builder()
                .id(w.getId())
                .name(w.getName())
                .location(w.getLocation())
                .address(w.getAddress())
                .geoZoneId(w.getGeoZoneId() != null ? w.getGeoZoneId().toString() : null)
                .active(w.isActive())
                .capacity(w.getCapacity())

                .adminUserIds(ids)
                .build();
    }
}
