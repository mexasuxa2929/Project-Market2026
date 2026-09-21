package mexa.club.analyticsservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.analytics")
public class AnalyticsProperties {

    private int cacheTtlMinutes = 5;
    private int lowStockThreshold = 10;
    private String warehouseInternalApiKey = "change-me-internal";

    public int getCacheTtlMinutes() {
        return cacheTtlMinutes;
    }

    public void setCacheTtlMinutes(int cacheTtlMinutes) {
        this.cacheTtlMinutes = cacheTtlMinutes;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getWarehouseInternalApiKey() {
        return warehouseInternalApiKey;
    }

    public void setWarehouseInternalApiKey(String warehouseInternalApiKey) {
        this.warehouseInternalApiKey = warehouseInternalApiKey;
    }
}
