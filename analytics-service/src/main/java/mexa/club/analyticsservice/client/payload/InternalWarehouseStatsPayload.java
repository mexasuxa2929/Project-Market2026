package mexa.club.analyticsservice.client.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalWarehouseStatsPayload(List<WarehouseLowStockItemPayload> lowStockItems, int totalLowStock) {}
