package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.dto.AuditLogResponse;
import mexa.club.warehouseproject.entity.AuditLog;
import mexa.club.warehouseproject.repository.AuditLogRepository;
import mexa.club.warehouseproject.security.WarehouseAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final WarehouseAccessService warehouseAccessService;

    public AuditLogService(AuditLogRepository auditLogRepository, WarehouseAccessService warehouseAccessService) {
        this.auditLogRepository = auditLogRepository;
        this.warehouseAccessService = warehouseAccessService;
    }

    @Transactional
    public void log(String action, String targetType, String targetId, UUID warehouseId, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setWarehouseId(warehouseId);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        log.setActorUsername(resolveCurrentUsername());
        log.setActorUserId(resolveCurrentUserIdOrNull());
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(String targetType, String targetId, UUID warehouseId, Pageable pageable) {
        String type = normalize(targetType);
        String id = normalize(targetId);
        return auditLogRepository.search(type, id, warehouseId, pageable).map(AuditLogResponse::fromEntity);
    }

    private UUID resolveCurrentUserIdOrNull() {
        try {
            return warehouseAccessService.requireCurrentUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    private static String normalize(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
