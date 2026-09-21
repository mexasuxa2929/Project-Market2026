package mexa.club.analyticsservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "revenue_daily")
public class RevenueDaily {

    @Id
    @Column(name = "revenue_date")
    private LocalDate revenueDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "order_count", nullable = false)
    private long orderCount;

    public LocalDate getRevenueDate() {
        return revenueDate;
    }

    public void setRevenueDate(LocalDate revenueDate) {
        this.revenueDate = revenueDate;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }
}
