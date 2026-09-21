package mexa.club.warehouseproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mexa.club.warehouseproject.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private UUID id;
    private String action;
    private String targetType;
    private String targetId;
    private UUID warehouseId;
    private UUID actorUserId;
    private String actorUsername;
    private String details;
    private LocalDateTime createdAt;

    public static AuditLogResponse fromEntity(AuditLog e) {
        return AuditLogResponse.builder()
                .id(e.getId())
                .action(e.getAction())
                .targetType(e.getTargetType())
                .targetId(e.getTargetId())
                .warehouseId(e.getWarehouseId())
                .actorUserId(e.getActorUserId())
                .actorUsername(e.getActorUsername())
                .details(e.getDetails())
                .createdAt(e.getCreatedAt())
                .build();
    }
}

