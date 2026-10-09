package mexa.club.warehouseproject.dto;

/**
 * Omborlar ro'yxati sahifalangandan keyin ham to'g'ri ko'rinadigan
 * global statistika (T-H4): jami/fanol soni, umumiy sig'im, kam zaxira.
 */
public record WarehouseStatsResponse(
        long totalCount,
        long activeCount,
        long totalCapacity,
        long totalLowStock
) {}
