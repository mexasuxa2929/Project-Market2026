package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.dto.OperatingExpenseRequest;
import mexa.club.warehouseproject.dto.OperatingExpenseResponse;
import mexa.club.warehouseproject.entity.OperatingExpense;
import mexa.club.warehouseproject.entity.Warehouse;
import mexa.club.warehouseproject.exception.ResourceNotFoundException;
import mexa.club.warehouseproject.repository.OperatingExpenseRepository;
import mexa.club.warehouseproject.repository.WarehouseRepository;
import mexa.club.warehouseproject.security.WarehouseAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OperatingExpenseService {

    private final OperatingExpenseRepository expenseRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseAccessService warehouseAccessService;

    public OperatingExpenseService(OperatingExpenseRepository expenseRepository,
                                   WarehouseRepository warehouseRepository,
                                   WarehouseAccessService warehouseAccessService) {
        this.expenseRepository = expenseRepository;
        this.warehouseRepository = warehouseRepository;
        this.warehouseAccessService = warehouseAccessService;
    }

    @Transactional(readOnly = true)
    public Page<OperatingExpenseResponse> list(UUID warehouseId, Pageable pageable) {
        warehouseAccessService.requireCanViewWarehouseOperations(warehouseId);
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId));
        }
        return expenseRepository.findByWarehouseId(warehouseId, pageable).map(OperatingExpenseResponse::fromEntity);
    }

    @Transactional
    public OperatingExpenseResponse create(UUID warehouseId, OperatingExpenseRequest dto, UUID actorId) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", String.valueOf(warehouseId)));

        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        if (dto.getCategory() == null) {
            throw new IllegalArgumentException("category is required");
        }

        OperatingExpense e = new OperatingExpense();
        e.setWarehouse(warehouse);
        e.setCategory(dto.getCategory());
        e.setAmount(dto.getAmount());
        e.setCurrency(dto.getCurrency() != null && !dto.getCurrency().isBlank() ? dto.getCurrency() : "UZS");
        e.setDescription(dto.getDescription());
        e.setIncurredAt(dto.getIncurredAt() != null ? dto.getIncurredAt() : LocalDateTime.now());
        e.setCreatedBy(actorId);
        OperatingExpense saved = expenseRepository.save(e);
        return OperatingExpenseResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID warehouseId, UUID expenseId) {
        warehouseAccessService.requireCanManageWarehouse(warehouseId);
        OperatingExpense e = expenseRepository.findByIdAndWarehouseId(expenseId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("OperatingExpense", String.valueOf(expenseId)));
        expenseRepository.delete(e);
    }
}
