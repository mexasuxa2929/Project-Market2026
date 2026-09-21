package mexa.club.analyticsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import mexa.club.analyticsservice.client.AuthInternalFeignClient;
import mexa.club.analyticsservice.client.DiscountInternalFeignClient;
import mexa.club.analyticsservice.client.OrderInternalFeignClient;
import mexa.club.analyticsservice.client.WarehouseInternalFeignClient;
import mexa.club.analyticsservice.client.payload.AuthGrowthApiEnvelope;
import mexa.club.analyticsservice.client.payload.AuthStatsApiEnvelope;
import mexa.club.analyticsservice.client.payload.DiscountApiEnvelope;
import mexa.club.analyticsservice.client.payload.InternalAuthStatsPayload;
import mexa.club.analyticsservice.client.payload.InternalDiscountStatsPayload;
import mexa.club.analyticsservice.client.payload.InternalOrderStatsPayload;
import mexa.club.analyticsservice.client.payload.InternalUserGrowthPointPayload;
import mexa.club.analyticsservice.client.payload.InternalWarehouseStatsPayload;
import mexa.club.analyticsservice.client.payload.TopPromotionPayload;
import mexa.club.analyticsservice.client.payload.WarehouseLowStockItemPayload;
import mexa.club.analyticsservice.client.payload.OrderApiEnvelope;
import mexa.club.analyticsservice.client.payload.TopProductStatPayload;
import mexa.club.analyticsservice.config.AnalyticsProperties;
import mexa.club.analyticsservice.dto.DashboardSummaryResponse;
import mexa.club.analyticsservice.dto.DiscountSummaryResponse;
import mexa.club.analyticsservice.dto.OrderStatusBreakdownResponse;
import mexa.club.analyticsservice.dto.RevenueChartResponse;
import mexa.club.analyticsservice.dto.StockAlertResponse;
import mexa.club.analyticsservice.dto.TopProductsResponse;
import mexa.club.analyticsservice.dto.UserGrowthResponse;
import mexa.club.analyticsservice.entity.DashboardSnapshot;
import mexa.club.analyticsservice.entity.RevenueDaily;
import mexa.club.analyticsservice.entity.TopProductCacheRow;
import mexa.club.analyticsservice.exception.AnalyticsException;
import mexa.club.analyticsservice.repository.DashboardSnapshotRepository;
import mexa.club.analyticsservice.repository.RevenueDailyRepository;
import mexa.club.analyticsservice.repository.TopProductCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private static final String SNAPSHOT_DAILY = "daily";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final OrderInternalFeignClient orderInternalFeignClient;
    private final AuthInternalFeignClient authInternalFeignClient;
    private final WarehouseInternalFeignClient warehouseInternalFeignClient;
    private final DiscountInternalFeignClient discountInternalFeignClient;
    private final DashboardSnapshotRepository dashboardSnapshotRepository;
    private final RevenueDailyRepository revenueDailyRepository;
    private final TopProductCacheRepository topProductCacheRepository;
    private final ObjectMapper objectMapper;
    private final AnalyticsProperties analyticsProperties;
    private final Clock clock;

    public AnalyticsService(
            OrderInternalFeignClient orderInternalFeignClient,
            AuthInternalFeignClient authInternalFeignClient,
            WarehouseInternalFeignClient warehouseInternalFeignClient,
            DiscountInternalFeignClient discountInternalFeignClient,
            DashboardSnapshotRepository dashboardSnapshotRepository,
            RevenueDailyRepository revenueDailyRepository,
            TopProductCacheRepository topProductCacheRepository,
            ObjectMapper objectMapper,
            AnalyticsProperties analyticsProperties,
            Clock clock
    ) {
        this.orderInternalFeignClient = orderInternalFeignClient;
        this.authInternalFeignClient = authInternalFeignClient;
        this.warehouseInternalFeignClient = warehouseInternalFeignClient;
        this.discountInternalFeignClient = discountInternalFeignClient;
        this.dashboardSnapshotRepository = dashboardSnapshotRepository;
        this.revenueDailyRepository = revenueDailyRepository;
        this.topProductCacheRepository = topProductCacheRepository;
        this.objectMapper = objectMapper;
        this.analyticsProperties = analyticsProperties;
        this.clock = clock;
    }

    public DashboardSummaryResponse getCachedSummaryOrCompute(boolean refresh) {
        if (refresh) {
            return computeDailySummaryAndPersist();
        }
        Optional<DashboardSnapshot> snap = dashboardSnapshotRepository.findById(SNAPSHOT_DAILY);
        if (snap.isPresent()) {
            Instant computed = snap.get().getComputedAt();
            long ttlMinutes = analyticsProperties.getCacheTtlMinutes();
            if (Duration.between(computed, Instant.now(clock)).toMinutes() < ttlMinutes) {
                try {
                    return objectMapper.readValue(snap.get().getPayloadJson(), DashboardSummaryResponse.class);
                } catch (Exception e) {
                    log.warn("Dashboard snapshot corrupt, recomputing", e);
                }
            }
        }
        return computeDailySummaryAndPersist();
    }

    public DashboardSummaryResponse computeDailySummaryAndPersist() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        LocalDateTime weekStart = today.minusDays(6).atStartOfDay();

        InternalOrderStatsPayload orderToday = safeOrderStats(todayStart, todayEnd);
        InternalOrderStatsPayload orderWeek = safeOrderStats(weekStart, todayEnd);
        InternalAuthStatsPayload authToday = safeAuthStats(todayStart, todayEnd);
        InternalWarehouseStatsPayload warehouse = safeWarehouse(analyticsProperties.getLowStockThreshold());
        InternalDiscountStatsPayload discountToday = safeDiscount(todayStart, todayEnd);

        Map<String, Long> breakdownToday = orderToday != null && orderToday.statusBreakdown() != null
                ? new LinkedHashMap<>(orderToday.statusBreakdown())
                : new LinkedHashMap<>();

        DashboardSummaryResponse summary = new DashboardSummaryResponse(
                Instant.now(clock),
                orderToday != null ? orderToday.orderCount() : 0L,
                orderToday != null && orderToday.revenue() != null ? orderToday.revenue() : ZERO,
                orderWeek != null ? orderWeek.orderCount() : 0L,
                orderWeek != null && orderWeek.revenue() != null ? orderWeek.revenue() : ZERO,
                authToday != null ? authToday.totalUsers() : 0L,
                authToday != null ? authToday.newUsersInPeriod() : 0L,
                authToday != null ? authToday.verifiedUsers() : 0L,
                warehouse != null ? warehouse.totalLowStock() : 0,
                discountToday != null ? discountToday.activePromotions() : 0L,
                discountToday != null && discountToday.totalDiscountGiven() != null
                        ? discountToday.totalDiscountGiven()
                        : ZERO,
                breakdownToday
        );

        try {
            DashboardSnapshot snap = new DashboardSnapshot();
            snap.setSnapshotKey(SNAPSHOT_DAILY);
            snap.setPayloadJson(objectMapper.writeValueAsString(summary));
            snap.setComputedAt(summary.computedAt());
            dashboardSnapshotRepository.save(snap);
        } catch (Exception e) {
            log.warn("Failed to persist dashboard snapshot", e);
        }

        upsertRevenueToday(
                orderToday != null && orderToday.revenue() != null ? orderToday.revenue() : ZERO,
                orderToday != null ? orderToday.orderCount() : 0L,
                today
        );

        return summary;
    }

    @Transactional
    public void computeAndSaveDailyRevenue() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        LocalDateTime from = yesterday.atStartOfDay();
        LocalDateTime to = yesterday.atTime(LocalTime.MAX);
        InternalOrderStatsPayload stats = safeOrderStats(from, to);
        if (stats == null) {
            return;
        }
        RevenueDaily row = revenueDailyRepository.findById(yesterday).orElseGet(RevenueDaily::new);
        row.setRevenueDate(yesterday);
        row.setRevenue(stats.revenue() != null ? stats.revenue() : ZERO);
        row.setOrderCount(stats.orderCount());
        revenueDailyRepository.save(row);
    }

    @Transactional
    public void refreshTopProducts(String period) {
        LocalDateTime[] range = orderRangeForRelativePeriod(period);
        InternalOrderStatsPayload stats = safeOrderStats(range[0], range[1]);
        topProductCacheRepository.deleteByPeriod(period);
        if (stats == null || stats.topProductIds() == null || stats.topProductIds().isEmpty()) {
            return;
        }
        Instant now = Instant.now(clock);
        List<TopProductCacheRow> rows = new ArrayList<>();
        int rank = 1;
        for (TopProductStatPayload row : stats.topProductIds()) {
            if (rank > 50) {
                break;
            }
            TopProductCacheRow entity = new TopProductCacheRow();
            entity.setPeriod(period);
            entity.setRankPosition(rank++);
            entity.setProductId(row.productId());
            entity.setProductName(row.productName() != null ? row.productName() : "");
            entity.setSoldCount(row.count());
            entity.setRevenue(row.revenue() != null ? row.revenue() : ZERO);
            entity.setUpdatedAt(now);
            rows.add(entity);
        }
        topProductCacheRepository.saveAll(rows);
    }

    public RevenueChartResponse revenueChart(String period) {
        LocalDate end = LocalDate.now(clock);
        LocalDate start = chartRangeStart(period, end);
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        revenueDailyRepository.findByRevenueDateBetweenOrderByRevenueDateAsc(start, end)
                .forEach(row -> byDay.put(row.getRevenueDate(),
                        row.getRevenue() != null ? row.getRevenue() : ZERO));

        List<RevenueChartResponse.Point> points = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            points.add(new RevenueChartResponse.Point(d, byDay.getOrDefault(d, ZERO)));
        }
        return new RevenueChartResponse(period, points);
    }

    public OrderStatusBreakdownResponse orderBreakdown(String period) {
        LocalDateTime[] range = orderRangeForRelativePeriod(period);
        InternalOrderStatsPayload stats = safeOrderStats(range[0], range[1]);
        Map<String, Long> breakdown = new LinkedHashMap<>();
        long total = 0;
        long delivered = 0;
        if (stats != null && stats.statusBreakdown() != null) {
            breakdown.putAll(stats.statusBreakdown());
            total = stats.orderCount();
            delivered = stats.statusBreakdown().getOrDefault("DELIVERED", 0L);
        }
        double conversion = total > 0 ? delivered * 100.0 / total : 0.0;
        return new OrderStatusBreakdownResponse(period, breakdown, roundOneDecimal(conversion));
    }

    public TopProductsResponse productsTop(String period, int limit) {
        int capped = Math.min(50, Math.max(1, limit));
        List<TopProductCacheRow> rows = topProductCacheRepository.findByPeriodOrderByRankPositionAsc(period);
        List<TopProductsResponse.Item> items = rows.stream()
                .limit(capped)
                .map(r -> new TopProductsResponse.Item(
                        r.getRankPosition(),
                        r.getProductId(),
                        r.getProductName(),
                        r.getSoldCount(),
                        r.getRevenue() != null ? r.getRevenue() : ZERO
                ))
                .toList();
        return new TopProductsResponse(period, items);
    }

    public StockAlertResponse stockAlerts(int threshold) {
        InternalWarehouseStatsPayload stats = safeWarehouse(threshold);
        if (stats == null || stats.lowStockItems() == null) {
            return new StockAlertResponse(threshold, List.of(), 0);
        }
        List<StockAlertResponse.Item> items = stats.lowStockItems().stream()
                .map(AnalyticsService::toStockItem)
                .toList();
        return new StockAlertResponse(threshold, items, stats.totalLowStock());
    }

    public UserGrowthResponse userGrowth(String period) {
        LocalDate end = LocalDate.now(clock);
        LocalDate start = switch (period.toLowerCase()) {
            case "today", "daily" -> end;
            case "week", "weekly", "7d", "last_7_days" -> end.minusDays(6);
            case "month", "monthly", "30d", "last_30_days" -> end.minusDays(29);
            case "quarterly", "90d", "last_90_days" -> end.minusDays(89);
            case "yearly", "year", "this_year" -> LocalDate.of(Year.now(clock).getValue(), 1, 1);
            default -> {
                try {
                    yield LocalDate.parse(period);
                } catch (Exception e) {
                    throw new AnalyticsException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", period);
                }
            }
        };
        List<InternalUserGrowthPointPayload> raw = safeAuthGrowth(start, end);
        List<UserGrowthResponse.Point> points = raw.stream()
                .map(p -> new UserGrowthResponse.Point(p.date(), p.newUsers(), p.totalUsers()))
                .toList();
        return new UserGrowthResponse(period, points);
    }

    public DiscountSummaryResponse discountSummary() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        LocalDateTime monthStart = today.minusDays(29).atStartOfDay();

        InternalDiscountStatsPayload todayStats = safeDiscount(todayStart, todayEnd);
        InternalDiscountStatsPayload monthStats = safeDiscount(monthStart, todayEnd);

        long active = todayStats != null ? todayStats.activePromotions() : 0;
        BigDecimal givenToday = todayStats != null && todayStats.totalDiscountGiven() != null
                ? todayStats.totalDiscountGiven()
                : ZERO;

        List<DiscountSummaryResponse.TopCoupon> top = List.of();
        if (monthStats != null && monthStats.topPromotions() != null) {
            top = monthStats.topPromotions().stream()
                    .map(AnalyticsService::toTopCoupon)
                    .toList();
        }
        return new DiscountSummaryResponse(active, givenToday, top);
    }

    private LocalDate chartRangeStart(String period, LocalDate end) {
        return switch (period.toLowerCase()) {
            case "last_7_days", "7d", "weekly", "week" -> end.minusDays(6);
            case "last_30_days", "30d", "monthly", "month" -> end.minusDays(29);
            case "last_90_days", "90d", "quarterly" -> end.minusDays(89);
            case "this_year", "yearly", "year" -> LocalDate.of(Year.now(clock).getValue(), 1, 1);
            case "today", "daily" -> end;
            default -> {
                // ISO date format: 2026-05-20 → shu kundan hozirgacha
                try {
                    yield LocalDate.parse(period);
                } catch (Exception e) {
                    throw new AnalyticsException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", period);
                }
            }
        };
    }

    private LocalDateTime[] orderRangeForRelativePeriod(String period) {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime end = today.atTime(LocalTime.MAX);
        LocalDateTime start = switch (period.toLowerCase()) {
            case "today", "daily" -> today.atStartOfDay();
            case "week", "weekly", "7d", "last_7_days" -> today.minusDays(6).atStartOfDay();
            case "month", "monthly", "30d", "last_30_days" -> today.minusDays(29).atStartOfDay();
            case "quarterly", "90d", "last_90_days" -> today.minusDays(89).atStartOfDay();
            case "yearly", "year", "this_year" -> LocalDate.of(Year.now(clock).getValue(), 1, 1).atStartOfDay();
            default -> {
                try {
                    yield LocalDate.parse(period).atStartOfDay();
                } catch (Exception e) {
                    throw new AnalyticsException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", period);
                }
            }
        };
        return new LocalDateTime[]{start, end};
    }

    private void upsertRevenueToday(BigDecimal revenue, long orderCount, LocalDate day) {
        RevenueDaily row = revenueDailyRepository.findById(day).orElseGet(RevenueDaily::new);
        row.setRevenueDate(day);
        row.setRevenue(revenue);
        row.setOrderCount(orderCount);
        revenueDailyRepository.save(row);
    }

    private InternalOrderStatsPayload safeOrderStats(LocalDateTime from, LocalDateTime to) {
        try {
            OrderApiEnvelope env = orderInternalFeignClient.stats(from, to);
            if (env != null && Boolean.TRUE.equals(env.success()) && env.data() != null) {
                return env.data();
            }
            log.warn("Order stats returned empty payload");
            return null;
        } catch (Exception ex) {
            log.warn("Order stats failed: {}", ex.getMessage());
            return null;
        }
    }

    private InternalAuthStatsPayload safeAuthStats(LocalDateTime from, LocalDateTime to) {
        try {
            AuthStatsApiEnvelope env = authInternalFeignClient.stats(from, to);
            if (env != null && env.success() && env.data() != null) {
                return env.data();
            }
            return null;
        } catch (Exception ex) {
            log.warn("Auth stats failed: {}", ex.getMessage());
            return null;
        }
    }

    private InternalWarehouseStatsPayload safeWarehouse(int threshold) {
        try {
            return warehouseInternalFeignClient.stats(threshold);
        } catch (Exception ex) {
            log.warn("Warehouse stats failed: {}", ex.getMessage());
            return null;
        }
    }

    private InternalDiscountStatsPayload safeDiscount(LocalDateTime from, LocalDateTime to) {
        try {
            DiscountApiEnvelope env = discountInternalFeignClient.stats(from, to);
            if (env != null && env.success() && env.data() != null) {
                return env.data();
            }
            return null;
        } catch (Exception ex) {
            log.warn("Discount stats failed: {}", ex.getMessage());
            return null;
        }
    }

    private List<InternalUserGrowthPointPayload> safeAuthGrowth(LocalDate from, LocalDate to) {
        try {
            AuthGrowthApiEnvelope env = authInternalFeignClient.growth(from, to);
            if (env != null && env.success() && env.data() != null) {
                return env.data();
            }
            return List.of();
        } catch (Exception ex) {
            log.warn("Auth growth failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private static StockAlertResponse.Item toStockItem(WarehouseLowStockItemPayload i) {
        return new StockAlertResponse.Item(
                i.productId(),
                i.productName(),
                i.availableQuantity(),
                i.warehouseId()
        );
    }

    private static DiscountSummaryResponse.TopCoupon toTopCoupon(TopPromotionPayload p) {
        return new DiscountSummaryResponse.TopCoupon(
                p.code(),
                p.usageCount(),
                p.totalDiscount() != null ? p.totalDiscount() : ZERO
        );
    }

    private static double roundOneDecimal(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
