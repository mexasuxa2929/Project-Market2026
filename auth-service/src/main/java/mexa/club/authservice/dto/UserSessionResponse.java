package mexa.club.authservice.dto;

import lombok.Builder;
import lombok.Getter;
import mexa.club.authservice.entity.UserSession;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserSessionResponse {
    private UUID id;
    private String deviceInfo;
    private String ipAddress;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private boolean current;

    public static UserSessionResponse fromEntity(UserSession session, UUID currentSessionId) {
        return UserSessionResponse.builder()
                .id(session.getId())
                .deviceInfo(session.getDeviceInfo())
                .ipAddress(session.getIpAddress())
                .createdAt(session.getCreatedAt())
                .lastUsedAt(session.getLastUsedAt())
                .current(currentSessionId != null && currentSessionId.equals(session.getId()))
                .build();
    }
}
