package mexa.club.deliveryservice.pricing;

import lombok.RequiredArgsConstructor;
import mexa.club.deliveryservice.entity.CartIncentiveRule;
import mexa.club.deliveryservice.entity.FreeDeliveryRule;
import mexa.club.deliveryservice.repository.CartIncentiveRuleRepository;
import mexa.club.deliveryservice.repository.DeliveryRepository;
import mexa.club.deliveryservice.repository.FreeDeliveryRuleRepository;
import mexa.club.deliveryservice.repository.PricingEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Overview/Analytics KPI'lari (delivery_pricing_events asosida). */
@Service
@RequiredArgsConstructor
public class PricingAnalyticsService {

    private final PricingEventRepository eventRepository;
    private final FreeDeliveryRuleRepository freeRepository;
    private final CartIncentiveRuleRepository incentiveRepository;
    private final DeliveryRepository deliveryRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> overview(int days) {
        int d = Math.max(1, Math.min(days, 365));
        LocalDateTime since = LocalDateTime.now().minusDays(d);
        Map<String, Object> out = new LinkedHashMap<>();

        long orders = eventRepository.countSince(since);
        long freeOrders = eventRepository.countFreeSince(since);
        long incentiveOrders = eventRepository.countIncentiveSince(since);
        BigDecimal avgFee = nz(eventRepository.avgFee(since));
        BigDecimal avgCost = nz(eventRepository.avgCost(since));
        BigDecimal avgSubsidy = nz(eventRepository.avgSubsidy(since));
        BigDecimal avgMargin = nz(eventRepository.avgMargin(since));
        BigDecimal aov = nz(eventRepository.avgCart(since));
        BigDecimal avgDistance = nz(eventRepository.avgDistance(since));
        BigDecimal revenue = nz(eventRepository.sumFee(since));
        BigDecimal cost = nz(eventRepository.sumCost(since));

        out.put("days", d);
        out.put("orders", orders);
        out.put("avgDeliveryFee", r2(avgFee));
        out.put("avgDeliveryCost", r2(avgCost));
        out.put("avgDeliverySubsidy", r2(avgSubsidy));
        out.put("avgDeliveryMargin", r2(avgMargin));
        out.put("averageOrderValue", r2(aov));
        out.put("freeDeliveryOrders", freeOrders);
        out.put("freeDeliveryRate", percent(freeOrders, orders));
        out.put("incentiveOrders", incentiveOrders);
        out.put("incentiveRate", percent(incentiveOrders, orders));
        out.put("avgDeliveryDistance", avgDistance.setScale(1, RoundingMode.HALF_UP));
        out.put("deliveryRevenue", r2(revenue));
        out.put("deliveryCostTotal", r2(cost));
        out.put("deliverySubsidyTotal", r2(cost.subtract(revenue).max(BigDecimal.ZERO)));
        out.put("thresholds", thresholdPerformance(since));
        out.put("deliveriesTotal", deliveryRepository.count());
        // D-H6: sample ro'yxat o'rniga DB'da sanoq.
        out.put("eventsSample", eventRepository.countRecent(since));
        // D-H4: eventlar qaysi source'dan (CREATE / QUOTE / INCENTIVE / LEGACY_QUOTE).
        Map<String, Long> bySource = new LinkedHashMap<>();
        for (Object[] row : eventRepository.countBySource(since)) {
            bySource.put(String.valueOf(row[0]), (Long) row[1]);
        }
        out.put("eventsBySource", bySource);
        // D-H5: eng ko'p qo'llanilgan qoidalar va surcharge to'plamlari.
        out.put("topRules", topRows(eventRepository.topRules(since), "rule"));
        out.put("topSurchargeSets", topRows(eventRepository.topSurchargeSets(since), "surcharge"));
        return out;
    }

    private static List<Map<String, Object>> topRows(List<Object[]> rows, String keyName) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put(keyName, row[0]);
            m.put("count", row[1]);
            out.add(m);
        }
        return out;
    }

    /** Har bir threshold bo'yicha: exposed, reached, conversion, avg cart. */
    private List<Map<String, Object>> thresholdPerformance(LocalDateTime since) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<BigDecimal> thresholds = new ArrayList<>();
        for (FreeDeliveryRule r : freeRepository.findAllByOrderByMinOrderAsc()) {
            if (Boolean.TRUE.equals(r.getActive())) thresholds.add(r.getMinOrder());
        }
        for (CartIncentiveRule r : incentiveRepository.findAllByOrderByThresholdAmountAsc()) {
            if (Boolean.TRUE.equals(r.getActive())) thresholds.add(r.getThresholdAmount());
        }
        for (BigDecimal t : thresholds.stream().distinct().sorted().toList()) {
            long exposed = eventRepository.countExposed(since, t);
            long reached = Math.max(
                    eventRepository.countReachedFree(since, t),
                    eventRepository.countReachedDiscount(since, t));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("threshold", r2(t));
            row.put("exposed", exposed);
            row.put("reached", reached);
            row.put("conversion", percent(reached, exposed));
            row.put("avgCart", r2(nz(eventRepository.avgCartForThreshold(since, t))));
            rows.add(row);
        }
        return rows;
    }

    private static BigDecimal percent(long part, long whole) {
        if (whole <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part * 100.0 / whole).setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal r2(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(0, RoundingMode.HALF_UP);
    }
}
