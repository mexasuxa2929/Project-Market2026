package mexa.club.reportservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mexa.club.reportservice.client.AuthServiceClient;
import mexa.club.reportservice.client.OrderServiceClient;
import mexa.club.reportservice.client.WarehouseServiceClient;
import mexa.club.reportservice.dto.ApiResponse;
import mexa.club.reportservice.dto.DashboardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final OrderServiceClient orderClient;
    private final WarehouseServiceClient warehouseClient;
    private final AuthServiceClient authClient;
    private final ObjectMapper objectMapper;

    public ReportService(
            OrderServiceClient orderClient,
            WarehouseServiceClient warehouseClient,
            AuthServiceClient authClient,
            ObjectMapper objectMapper
    ) {
        this.orderClient = orderClient;
        this.warehouseClient = warehouseClient;
        this.authClient = authClient;
        this.objectMapper = objectMapper;
    }

    @Cacheable("dashboard")
    public DashboardResponse dashboard() {
        List<Map<String, Object>> orders = fetchPageSafe(() -> orderClient.all(0, 200), "order-service");
        List<Map<String, Object>> users = fetchPageSafe(() -> authClient.users(0, 200), "auth-service");
        List<Map<String, Object>> warehouses = fetchPageSafe(warehouseClient::warehouses, "warehouse-service");

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        long todayOrders = 0;
        BigDecimal todayRevenue = BigDecimal.ZERO;
        long monthOrders = 0;
        BigDecimal monthRevenue = BigDecimal.ZERO;
        long pendingOrders = 0;

        for (Map<String, Object> o : orders) {
            LocalDate created = parseDate(o.get("createdAt"));
            BigDecimal amount = parseMoney(o.get("totalAmount"));
            String status = String.valueOf(o.get("status"));
            if ("PENDING".equalsIgnoreCase(status)) {
                pendingOrders++;
            }
            if (created != null && created.isEqual(today)) {
                todayOrders++;
                todayRevenue = todayRevenue.add(amount);
            }
            if (created != null && !created.isBefore(monthStart)) {
                monthOrders++;
                monthRevenue = monthRevenue.add(amount);
            }
        }

        long todayUsers = users.stream().filter(u -> parseDate(u.get("createdAt")) != null && parseDate(u.get("createdAt")).isEqual(today)).count();
        long todayDeliveries = 0; // placeholder from delivery service not yet connected here
        long lowStockAlerts = warehouses.stream()
                .mapToLong(w -> {
                    Object v = w.get("lowStockCount");
                    return v instanceof Number n ? n.longValue() : 0L;
                })
                .sum();

        Map<String, Long> topProductCounts = new HashMap<>();
        for (Map<String, Object> o : orders) {
            Object id = o.get("id");
            if (id != null) {
                topProductCounts.merge(String.valueOf(id), 1L, Long::sum);
            }
        }
        List<DashboardResponse.TopProduct> topProducts = topProductCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(e -> new DashboardResponse.TopProduct(UUID.nameUUIDFromBytes(e.getKey().getBytes()), "Product " + e.getKey().substring(0, 6), e.getValue()))
                .toList();

        return new DashboardResponse(
                new DashboardResponse.Period(todayOrders, todayRevenue, todayUsers, todayDeliveries),
                new DashboardResponse.Month(monthOrders, monthRevenue, topProducts),
                lowStockAlerts,
                pendingOrders
        );
    }

    @Cacheable("salesReports")
    public List<Map<String, Object>> sales(LocalDate from, LocalDate to) {
        List<Map<String, Object>> orders = fetchPageSafe(() -> orderClient.all(0, 500), "order-service");
        return filterByDate(orders, from, to);
    }

    @Cacheable("productReports")
    public List<Map<String, Object>> products(LocalDate from, LocalDate to) {
        List<Map<String, Object>> orders = fetchPageSafe(() -> orderClient.all(0, 500), "order-service");
        return filterByDate(orders, from, to);
    }

    @Cacheable("customerReports")
    public List<Map<String, Object>> customers(LocalDate from, LocalDate to) {
        List<Map<String, Object>> users = fetchPageSafe(() -> authClient.users(0, 500), "auth-service");
        return filterByDate(users, from, to);
    }

    private List<Map<String, Object>> fetchPageSafe(
            Supplier<ApiResponse<Map<String, Object>>> call,
            String upstreamName
    ) {
        try {
            ApiResponse<Map<String, Object>> response = call.get();
            return extractContent(response != null ? response.data() : null);
        } catch (Exception ex) {
            log.warn("Upstream {} unavailable, returning empty data. reason={}", upstreamName, ex.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractContent(Map<String, Object> map) {
        if (map == null) {
            return List.of();
        }
        Object content = map.get("content");
        if (content instanceof List<?> list) {
            return list.stream().filter(Map.class::isInstance).map(v -> (Map<String, Object>) v).toList();
        }
        return List.of();
    }

    private List<Map<String, Object>> filterByDate(List<Map<String, Object>> src, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return src;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : src) {
            LocalDate d = parseDate(row.get("createdAt"));
            if (d == null) {
                continue;
            }
            if (from != null && d.isBefore(from)) {
                continue;
            }
            if (to != null && d.isAfter(to)) {
                continue;
            }
            out.add(row);
        }
        return out;
    }

    private LocalDate parseDate(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(String.valueOf(raw)).toLocalDate();
        } catch (Exception ignored) {
            try {
                return LocalDate.parse(String.valueOf(raw));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private BigDecimal parseMoney(Object raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(raw));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
