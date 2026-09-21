package mexa.club.orderservice.dto;

import java.util.UUID;

/**
 * Mahsulot o'chirilishi oldidan unga bog'liq orderlar mavjudligini bildirish uchun.
 */
public class ProductUsageCheckResponse {

    private UUID productId;
    private boolean used;
    private long orderCount;

    public ProductUsageCheckResponse() {
    }

    public ProductUsageCheckResponse(UUID productId, boolean used, long orderCount) {
        this.productId = productId;
        this.used = used;
        this.orderCount = orderCount;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }
}
