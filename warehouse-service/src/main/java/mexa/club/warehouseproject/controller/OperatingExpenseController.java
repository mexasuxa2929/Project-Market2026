package mexa.club.warehouseproject.controller;

import mexa.club.warehouseproject.dto.OperatingExpenseRequest;
import mexa.club.warehouseproject.dto.OperatingExpenseResponse;
import mexa.club.warehouseproject.service.OperatingExpenseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouses/{warehouseId}/operating-expenses")
public class OperatingExpenseController {

    private final OperatingExpenseService expenseService;

    public OperatingExpenseController(OperatingExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public ResponseEntity<Page<OperatingExpenseResponse>> list(
            @PathVariable UUID warehouseId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<OperatingExpenseResponse> page = expenseService.list(warehouseId, pageable);
        return ResponseEntity.ok(page);
    }

    @PostMapping
    public ResponseEntity<OperatingExpenseResponse> create(
            @PathVariable UUID warehouseId,
            @Valid @RequestBody OperatingExpenseRequest request) {
        UUID actorId = currentUserId();
        OperatingExpenseResponse created = expenseService.create(warehouseId, request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID warehouseId,
            @PathVariable UUID expenseId) {
        expenseService.delete(warehouseId, expenseId);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UUID) {
                return (UUID) auth.getPrincipal();
            }
            // Fallback: try to parse name as UUID
            if (auth != null && auth.getName() != null) {
                return UUID.fromString(auth.getName());
            }
        } catch (Exception ignored) {}
        return null;
    }
}
