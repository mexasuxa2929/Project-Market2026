package mexa.club.warehouseproject.security;

import mexa.club.warehouseproject.repository.WarehouseAdminRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Ombor bo‘yicha: {@code SUPER_ADMIN} hammasi; {@code ROLE_ADMIN} faqat biriktirilgan omborlar;
 * oddiy {@code USER} o‘qish (ro‘yxat / qoldiq / kirimlar) — cheklovsiz.
 */
@Service
public class WarehouseAccessService {

    private final WarehouseAdminRepository warehouseAdminRepository;

    public WarehouseAccessService(
            WarehouseAdminRepository warehouseAdminRepository
    ) {
        this.warehouseAdminRepository = warehouseAdminRepository;
    }

    public UUID requireCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Not authenticated");
        }
        Object details = auth.getDetails();
        if (details instanceof JwtAuthDetails jwtAuthDetails && jwtAuthDetails.userId() != null) {
            return jwtAuthDetails.userId();
        }
        throw new AccessDeniedException("Token userId is missing");
    }

    public boolean hasSuperAdminRole() {
        return hasAuthority("ROLE_SUPER_ADMIN");
    }

    public boolean hasWarehouseAdminRole() {
        return hasAuthority("ROLE_ADMIN");
    }

    private boolean hasAuthority(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    /** Qoldiq, kirim yaratish, qoldiqni o‘zgartirish. */
    public void requireCanManageWarehouse(UUID warehouseId) {
        if (hasSuperAdminRole()) {
            return;
        }
        if (!hasWarehouseAdminRole()) {
            throw new AccessDeniedException("Warehouse management requires ADMIN or SUPER_ADMIN role");
        }
        UUID uid = requireCurrentUserId();
        if (!warehouseAdminRepository.existsByWarehouse_IdAndUserId(warehouseId, uid)) {
            throw new AccessDeniedException("Not assigned as administrator for warehouse id=" + warehouseId);
        }
    }

    /**
     * Ombor tarkibini ko‘rish: USER va SUPER_ADMIN — hamma ombor;
     * ROLE_ADMIN — faqat o‘ziga biriktirilgan omborlar.
     */
    public void requireCanViewWarehouseOperations(UUID warehouseId) {
        if (hasSuperAdminRole()) {
            return;
        }
        if (!hasWarehouseAdminRole()) {
            return;
        }
        UUID uid = requireCurrentUserId();
        if (!warehouseAdminRepository.existsByWarehouse_IdAndUserId(warehouseId, uid)) {
            throw new AccessDeniedException("Not assigned as administrator for warehouse id=" + warehouseId);
        }
    }
}
