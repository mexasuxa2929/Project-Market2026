package mexa.club.deliveryservice.pricing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mexa.club.deliveryservice.entity.CartIncentiveRule;
import mexa.club.deliveryservice.entity.DeliverySurcharge;
import mexa.club.deliveryservice.entity.DeliveryType;
import mexa.club.deliveryservice.entity.DeliveryZone;
import mexa.club.deliveryservice.entity.FreeDeliveryRule;
import mexa.club.deliveryservice.entity.PricingEvent;
import mexa.club.deliveryservice.entity.PricingSettings;
import mexa.club.deliveryservice.entity.PricingRule;
import mexa.club.deliveryservice.pricing.PricingDtos.IncentiveInfo;
import mexa.club.deliveryservice.pricing.PricingDtos.PricingQuote;
import mexa.club.deliveryservice.pricing.PricingDtos.PricingRequest;
import mexa.club.deliveryservice.pricing.PricingDtos.SurchargeLine;
import mexa.club.deliveryservice.repository.CartIncentiveRuleRepository;
import mexa.club.deliveryservice.repository.DeliverySurchargeRepository;
import mexa.club.deliveryservice.repository.DeliveryTypeRepository;
import mexa.club.deliveryservice.repository.DeliveryZoneRepository;
import mexa.club.deliveryservice.repository.FreeDeliveryRuleRepository;
import mexa.club.deliveryservice.repository.PricingEventRepository;
import mexa.club.deliveryservice.repository.PricingRuleRepository;
import mexa.club.deliveryservice.repository.PricingSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Delivery pricing engine (deterministik).
 *
 * <pre>
 *  fee = base + distance*perKm + weight*perKg
 *  fee = min(maxFee, max(minFee, fee))
 *  fee += surcharges (peak hour, og'irlik, express...)
 *  fee -= discount (free delivery / cart incentive)
 *  cost = baseCost + distance*perKmCost + weight*perKgCost  →  margin = fee - cost
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeliveryPricingService {

    private static final BigDecimal DEFAULT_BASE = new BigDecimal("8000");
    private static final BigDecimal DEFAULT_PER_KM = new BigDecimal("2000");
    private static final BigDecimal DEFAULT_PER_KG = new BigDecimal("0");
    private static final BigDecimal DEFAULT_MIN = new BigDecimal("8000");
    private static final BigDecimal DEFAULT_MAX = new BigDecimal("40000");

    private final DeliveryZoneRepository zoneRepository;
    private final PricingRuleRepository ruleRepository;
    private final FreeDeliveryRuleRepository freeDeliveryRuleRepository;
    private final CartIncentiveRuleRepository incentiveRuleRepository;
    private final DeliveryTypeRepository deliveryTypeRepository;
    private final DeliverySurchargeRepository surchargeRepository;
    private final PricingSettingsRepository settingsRepository;
    private final PricingEventRepository eventRepository;

    /** Asosiy hisoblash (D-H6: read-only — event repository'ning o'z tranzaksiyasida yoziladi). */
    public PricingQuote calculate(PricingRequest req) {
        LocalDateTime time = req.time() != null ? req.time() : LocalDateTime.now();
        DeliveryZone zone = resolveZone(req);
        DeliveryType type = req.deliveryType() == null || req.deliveryType().isBlank()
                ? null
                : deliveryTypeRepository.findByNameIgnoreCase(req.deliveryType().trim())
                        // D-H9: o'chirilgan (active=false) delivery type narxga ta'sir qilmaydi.
                        .filter(t -> Boolean.TRUE.equals(t.getActive()))
                        .orElse(null);
        // D-H6: har bir jadval quote ichida bir marta o'qiladi (takroriy to'liq skan yo'q).
        List<FreeDeliveryRule> freeRules = freeDeliveryRuleRepository.findAllByOrderByMinOrderAsc();
        List<CartIncentiveRule> incentiveRules = incentiveRuleRepository.findAllByOrderByThresholdAmountAsc();
        List<PricingRule> allRules = ruleRepository.findAllByOrderByPriorityDescCreatedAtDesc();
        List<DeliverySurcharge> allSurcharges = sortedSurcharges();
        PricingSettings settings = settingsRepository.findById(PricingSettings.DEFAULT_ID)
                .orElseGet(() -> settingsRepository.findAll().stream().findFirst().orElse(null));

        double km = req.distanceKm() != null && req.distanceKm() > 0 ? req.distanceKm() : 0.0;
        double kg = req.weightKg() != null && req.weightKg() > 0 ? req.weightKg() : 0.0;
        BigDecimal orderValue = req.orderValue();

        // ── 1. Base formula ────────────────────────────────────────────────
        PricingRule rule = pickRule(zone, type, time, allRules);
        BigDecimal base = pick(rule != null ? rule.getBaseFee() : null, zone != null ? zone.getFee() : null, DEFAULT_BASE);
        BigDecimal perKm = pick(rule != null ? rule.getPricePerKm() : null, zone != null ? zone.getPerKmFee() : null, DEFAULT_PER_KM);
        BigDecimal perKg = pick(rule != null ? rule.getPricePerKg() : null, zone != null ? zone.getPerKgFee() : null, DEFAULT_PER_KG);
        BigDecimal minFee = pick(rule != null ? rule.getMinFee() : null, zone != null ? zone.getMinFee() : null, DEFAULT_MIN);
        BigDecimal maxFee = firstPositive(rule != null ? rule.getMaxFee() : null, zone != null ? zone.getMaxFee() : null);

        BigDecimal distanceFee = perKm.multiply(BigDecimal.valueOf(km));
        BigDecimal weightFee = perKg.multiply(BigDecimal.valueOf(kg));
        BigDecimal calculated = base.add(distanceFee).add(weightFee);

        BigDecimal fee = calculated;
        if (maxFee != null && fee.compareTo(maxFee) > 0) fee = maxFee;
        if (fee.compareTo(minFee) < 0) fee = minFee;
        fee = round(fee);

        String appliedRule = rule != null ? rule.getName()
                : (zone != null ? "Zona: " + zone.getRegion() + "/" + zone.getDistrict() : "Default tarif");

        // ── 2. Delivery type qo'shimcha haqi ───────────────────────────────
        List<SurchargeLine> surchargeLines = new ArrayList<>();
        BigDecimal surchargeTotal = BigDecimal.ZERO;
        if (type != null && type.getAdditionalFee() != null && type.getAdditionalFee().signum() > 0) {
            surchargeLines.add(new SurchargeLine(type.getName(), "DELIVERY_TYPE", round(type.getAdditionalFee())));
            surchargeTotal = surchargeTotal.add(type.getAdditionalFee());
        }

        // ── 3. Surcharges ─────────────────────────────────────────────────
        List<String> appliedSurchargeNames = new ArrayList<>();
        for (DeliverySurcharge s : allSurcharges) {
            if (!matches(s, req, km, kg, orderValue, type, time)) continue;
            BigDecimal amount = "PERCENT".equalsIgnoreCase(s.getFeeType())
                    ? fee.multiply(s.getFee()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    : round(s.getFee());
            if (amount.signum() <= 0) continue;
            surchargeLines.add(new SurchargeLine(s.getName(), s.getConditionType(), amount));
            surchargeTotal = surchargeTotal.add(amount);
            if (s.getName() != null) appliedSurchargeNames.add(s.getName());
        }

        BigDecimal beforeDiscount = fee.add(surchargeTotal);
        // Surcharges (type qo'shimcha haqi, peak hour, og'irlik va h.k.) yakuniy narxga qo'shiladi.
        fee = round(beforeDiscount);
        BigDecimal discount = BigDecimal.ZERO;
        boolean free = false;
        String freeRuleName = null;
        FreeDeliveryRule applicableFree = null;
        CartIncentiveRule applicableIncentive = null;

        // ── 4. Free delivery: zona chegarasi (D-H10) yoki free-delivery qoidasi ──
        boolean zoneFree = zone != null && zone.getFreeAbove() != null && orderValue != null
                && orderValue.compareTo(zone.getFreeAbove()) >= 0;
        if (orderValue != null) {
            applicableFree = freeRules.stream()
                    .filter(r -> activeAt(r.getActive(), r.getStartAt(), r.getEndAt(), time))
                    .filter(r -> matchesZone(r.getZoneId(), zone))
                    .filter(r -> matchesType(r.getDeliveryType(), req.deliveryType()))
                    .filter(r -> orderValue.compareTo(r.getMinOrder()) >= 0)
                    .max(Comparator.comparing(FreeDeliveryRule::getMinOrder))
                    .orElse(null);
        }
        if (zoneFree || applicableFree != null) {
            free = true;
            freeRuleName = applicableFree != null ? applicableFree.getName()
                    : "Zona: " + zone.getRegion() + "/" + zone.getDistrict() + " (bepul chegarasi)";
            BigDecimal target = BigDecimal.ZERO;
            if (!zoneFree && applicableFree != null) {
                target = round(applicableFree.getDeliveryFee())
                        .max(BigDecimal.ZERO).min(beforeDiscount);
            }
            fee = target;
            discount = beforeDiscount.subtract(target);
        } else if (orderValue != null) {
            // ── 5. Cart incentive reward (AMOUNT / PERCENT / FREE) ────────
            applicableIncentive = incentiveRules.stream()
                    .filter(r -> activeAt(r.getActive(), r.getStartAt(), r.getEndAt(), time))
                    .filter(r -> orderValue.compareTo(r.getThresholdAmount()) >= 0)
                    .max(Comparator.comparing(CartIncentiveRule::getThresholdAmount))
                    .orElse(null);
            if (applicableIncentive != null) {
                BigDecimal reward = applicableIncentive.getRewardValue() != null
                        ? applicableIncentive.getRewardValue().max(BigDecimal.ZERO) : BigDecimal.ZERO;
                BigDecimal after;
                switch (applicableIncentive.getRewardType() == null ? "FREE" : applicableIncentive.getRewardType().toUpperCase()) {
                    case "AMOUNT" -> after = beforeDiscount.subtract(round(reward)).max(BigDecimal.ZERO);
                    case "PERCENT" -> after = beforeDiscount.subtract(
                            beforeDiscount.multiply(reward).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)).max(BigDecimal.ZERO);
                    default -> { after = BigDecimal.ZERO; free = true; }
                }
                discount = beforeDiscount.subtract(after);
                fee = round(after);
            }
        }
        if (discount.signum() < 0) discount = BigDecimal.ZERO;

        // ── 6. Real cost va margin ────────────────────────────────────────
        BigDecimal cost = BigDecimal.ZERO;
        if (settings != null) {
            cost = settings.getBaseCost().add(settings.getPerKmCost().multiply(BigDecimal.valueOf(km)))
                    .add(settings.getPerKgCost().multiply(BigDecimal.valueOf(kg)));
        }
        cost = round(cost);
        BigDecimal margin = fee.subtract(cost);

        // ── 7. Cart incentive tavsiyasi ───────────────────────────────────
        IncentiveInfo incentive = buildIncentive(orderValue, fee, time, zone, req.deliveryType(),
                freeRules, incentiveRules);

        // ── 8. Analytics event ────────────────────────────────────────────
        if (req.recordEvent()) {
            // D-H4: thresholdAmount — event qaysi threshold'ga exposed bo'lgani
            // (qoida hali bajarilmagan bo'lsa keyingi threshold).
            BigDecimal threshold = applicableFree != null ? applicableFree.getMinOrder()
                    : (applicableIncentive != null ? applicableIncentive.getThresholdAmount()
                    : (incentive != null ? incentive.nextThreshold() : null));
            recordEvent(req, zone, type, rule, base, distanceFee, weightFee, surchargeTotal, discount, fee, cost, margin,
                    free, threshold, req.source(), String.join(", ", appliedSurchargeNames));
        }

        Integer minutes = type != null ? type.getEstimatedMinutes() : null;
        Integer days = zone != null ? zone.getEstimatedDays() : 1;
        String message = incentive != null ? incentive.message()
                : (free ? "Bepul yetkazib berish qo'llanildi." : null);

        return new PricingQuote(
                round(base), round(distanceFee), round(weightFee),
                surchargeLines, round(surchargeTotal), round(discount), fee,
                cost, margin, free, appliedRule,
                zone != null ? zone.getRegion() + " / " + zone.getDistrict() : null,
                type != null ? type.getName() : (req.deliveryType() != null ? req.deliveryType() : "STANDARD"),
                minutes, days, incentive, message
        );
    }

    /** Faqat narx + taxminiy kun (eski /api/delivery/fee endpointlari uchun). */
    public PricingQuote quote(String region, String district, Double distanceKm, Double weightKg, BigDecimal orderValue) {
        return calculate(new PricingRequest(null, region, district, distanceKm, weightKg, orderValue, null,
                LocalDateTime.now(), null, false, "QUOTE"));
    }

    // ─── Incentive ─────────────────────────────────────────────────────────

    private IncentiveInfo buildIncentive(BigDecimal orderValue, BigDecimal currentFee, LocalDateTime time,
                                          DeliveryZone zone, String deliveryType,
                                          List<FreeDeliveryRule> freeRules,
                                          List<CartIncentiveRule> incentiveRules) {
        if (orderValue == null) return null;
        record Threshold(String name, BigDecimal amount, String rewardType, BigDecimal rewardValue, String template) {}

        List<Threshold> thresholds = new ArrayList<>();
        for (FreeDeliveryRule r : freeRules) {
            if (!activeAt(r.getActive(), r.getStartAt(), r.getEndAt(), time)) continue;
            if (!matchesZone(r.getZoneId(), zone) || !matchesType(r.getDeliveryType(), deliveryType)) continue;
            thresholds.add(new Threshold(r.getName(), r.getMinOrder(), "FREE", BigDecimal.ZERO,
                    "Yana {remaining} so'mlik mahsulot qo'shing va bepul yetkazib berishdan foydalaning."));
        }
        for (CartIncentiveRule r : incentiveRules) {
            if (!activeAt(r.getActive(), r.getStartAt(), r.getEndAt(), time)) continue;
            thresholds.add(new Threshold(r.getName(), r.getThresholdAmount(), r.getRewardType(), r.getRewardValue(),
                    r.getMessageTemplate()));
        }
        Optional<Threshold> next = thresholds.stream()
                .filter(t -> t.amount().compareTo(orderValue) > 0)
                .min(Comparator.comparing(Threshold::amount));
        if (next.isEmpty()) return null;

        Threshold t = next.get();
        BigDecimal remaining = t.amount().subtract(orderValue);
        BigDecimal rewardValue = t.rewardValue() != null
                ? t.rewardValue().max(BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal feeAfter;
        switch (t.rewardType() == null ? "FREE" : t.rewardType().toUpperCase()) {
            case "AMOUNT" -> feeAfter = currentFee.subtract(rewardValue).max(BigDecimal.ZERO);
            case "PERCENT" -> feeAfter = currentFee.subtract(
                    currentFee.multiply(rewardValue).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)).max(BigDecimal.ZERO);
            default -> feeAfter = BigDecimal.ZERO;
        }
        BigDecimal saving = currentFee.subtract(feeAfter).max(BigDecimal.ZERO);
        String message = t.template()
                .replace("{remaining}", formatSum(remaining))
                .replace("{saving}", formatSum(saving));
        return new IncentiveInfo(orderValue, t.amount(), remaining, currentFee, feeAfter, saving, message);
    }

    private static String formatSum(BigDecimal v) {
        return v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    // ─── Yordamchilar ──────────────────────────────────────────────────────

    private DeliveryZone resolveZone(PricingRequest req) {
        if (req.zoneId() != null) {
            return zoneRepository.findById(req.zoneId()).orElse(null);
        }
        if (req.region() != null && req.district() != null
                && !req.region().isBlank() && !req.district().isBlank()) {
            return zoneRepository.findByRegionIgnoreCaseAndDistrictIgnoreCase(req.region().trim(), req.district().trim())
                    .orElse(null);
        }
        return null;
    }

    private PricingRule pickRule(DeliveryZone zone, DeliveryType type, LocalDateTime time, List<PricingRule> allRules) {
        return allRules.stream()
                .filter(r -> activeAt(r.getActive(), r.getStartAt(), r.getEndAt(), time))
                .filter(r -> r.getZoneId() == null || (zone != null && r.getZoneId().equals(zone.getId())))
                .filter(r -> r.getDeliveryType() == null || r.getDeliveryType().isBlank()
                        || (type != null && r.getDeliveryType().equalsIgnoreCase(type.getName())))
                .filter(DeliveryPricingService::configured)
                .findFirst()
                .orElse(null);
    }

    /** Barcha fee maydonlari null/0 bo'lgan qoida sozlanmagan hisoblanadi (0 = not configured). */
    private static boolean configured(PricingRule r) {
        return positiveOrNull(r.getBaseFee()) != null
                || positiveOrNull(r.getPricePerKm()) != null
                || positiveOrNull(r.getPricePerKg()) != null
                || positiveOrNull(r.getMinFee()) != null
                || positiveOrNull(r.getMaxFee()) != null;
    }

    private List<DeliverySurcharge> sortedSurcharges() {
        List<DeliverySurcharge> list = new ArrayList<>(surchargeRepository.findAllByOrderByPriorityDescCreatedAtAsc());
        list.sort(Comparator.comparing((DeliverySurcharge s) -> s.getPriority() == null ? 100 : s.getPriority()).reversed());
        return list;
    }

    private boolean matches(DeliverySurcharge s, PricingRequest req, double km, double kg,
                             BigDecimal orderValue, DeliveryType type, LocalDateTime time) {
        if (!Boolean.TRUE.equals(s.getActive())) return false;
        // D-H2: sana oralig'i — startAt/endAt belgilangan bo'lsa surcharge faqat shu davrda faol
        // (HOLIDAY butun yil emas, faqat berilgan sanalar oralig'ida ishlaydi).
        if (s.getStartAt() != null && time.isBefore(s.getStartAt())) return false;
        if (s.getEndAt() != null && time.isAfter(s.getEndAt())) return false;
        String cond = s.getConditionType() == null ? "" : s.getConditionType().toUpperCase();
        String op = s.getOperator() == null ? "GTE" : s.getOperator().toUpperCase();
        switch (cond) {
            case "PEAK_HOUR", "NIGHT", "HOLIDAY" -> {
                if (s.getTimeFrom() == null || s.getTimeTo() == null) return false;
                LocalTime t = time.toLocalTime();
                LocalTime from = s.getTimeFrom(), to = s.getTimeTo();
                boolean inWindow = from.isBefore(to)
                        ? !t.isBefore(from) && t.isBefore(to)
                        : (!t.isBefore(from) || t.isBefore(to));
                if (!inWindow) return false;
            }
            case "HEAVY_ORDER" -> {
                if (s.getWeightFrom() == null && s.getWeightTo() == null) return false;
                BigDecimal w = BigDecimal.valueOf(kg);
                if (s.getWeightFrom() != null && w.compareTo(s.getWeightFrom()) < 0) return false;
                if (s.getWeightTo() != null && w.compareTo(s.getWeightTo()) >= 0) return false;
            }
            case "EXPRESS" -> {
                if (s.getDeliveryType() != null && !s.getDeliveryType().equalsIgnoreCase(req.deliveryType())) return false;
                if (type == null || !"EXPRESS".equalsIgnoreCase(type.getName())) return false;
            }
            // D-H3: UI taklif qilgan DISTANCE/WEIGHT endi shu qiymatlarning o'zi bilan
            // solishtiriladi (avval default → orderValue ga tushib, "masofa > 10 km"
            // qoidasi aslida "savat ≥ 10 so'm" bo'lib ishlagan).
            case "DISTANCE" -> {
                if (!matchesScalar(BigDecimal.valueOf(km), s, op)) return false;
            }
            case "WEIGHT" -> {
                if (!matchesScalar(BigDecimal.valueOf(kg), s, op)) return false;
            }
            default -> {
                // HIGH_DEMAND / REMOTE_AREA / WEATHER / boshqa: orderValue bo'yicha operator
                if (s.getConditionValue() != null && orderValue != null) {
                    if (!matchesScalar(orderValue, s, op)) return false;
                }
            }
        }
        if (s.getDeliveryType() != null && !s.getDeliveryType().isBlank()
                && !s.getDeliveryType().equalsIgnoreCase(req.deliveryType())) {
            return false;
        }
        return true;
    }

    /**
     * D-H3: operator bo'yicha solishtirish. BETWEEN → [weightFrom..weightTo] oralig'i
     * (min/max sifatida qayta ishlatiladi, qo'shimcha ustun shart emas).
     */
    private static boolean matchesScalar(BigDecimal actual, DeliverySurcharge s, String op) {
        if ("BETWEEN".equals(op)) {
            if (s.getWeightFrom() == null || s.getWeightTo() == null) return false;
            return actual.compareTo(s.getWeightFrom()) >= 0 && actual.compareTo(s.getWeightTo()) <= 0;
        }
        if (s.getConditionValue() == null) return true;
        return compare(actual, op, s.getConditionValue());
    }

    private static boolean compare(BigDecimal actual, String operator, BigDecimal value) {
        int c = actual.compareTo(value);
        return switch (operator == null ? "GTE" : operator.toUpperCase()) {
            case "GT" -> c > 0;
            case "LT" -> c < 0;
            case "LTE" -> c <= 0;
            case "EQ" -> c == 0;
            default -> c >= 0;
        };
    }

    private static boolean activeAt(Boolean active, LocalDateTime start, LocalDateTime end, LocalDateTime time) {
        if (active != null && !active) return false;
        if (start != null && time.isBefore(start)) return false;
        if (end != null && time.isAfter(end)) return false;
        return true;
    }

    private static boolean matchesZone(UUID ruleZoneId, DeliveryZone zone) {
        return ruleZoneId == null || (zone != null && ruleZoneId.equals(zone.getId()));
    }

    private static boolean matchesType(String ruleType, String requestedType) {
        if (ruleType == null || ruleType.isBlank()) return true;
        return requestedType != null && ruleType.equalsIgnoreCase(requestedType);
    }

    private static BigDecimal pick(BigDecimal primary, BigDecimal fallback, BigDecimal def) {
        if (positiveOrNull(primary) != null) return primary;
        if (positiveOrNull(fallback) != null) return fallback;
        return def;
    }

    private static BigDecimal firstPositive(BigDecimal a, BigDecimal b) {
        return positiveOrNull(a) != null ? a : positiveOrNull(b);
    }

    private static BigDecimal positiveOrNull(BigDecimal v) {
        return v != null && v.signum() > 0 ? v : null;
    }

    private static BigDecimal round(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(0, RoundingMode.HALF_UP);
    }

    private void recordEvent(PricingRequest req, DeliveryZone zone, DeliveryType type, PricingRule rule,
                             BigDecimal base, BigDecimal distanceFee, BigDecimal weightFee,
                             BigDecimal surchargeTotal, BigDecimal discount, BigDecimal fee,
                             BigDecimal cost, BigDecimal margin, boolean free, BigDecimal threshold,
                             String source, String surchargeNames) {
        try {
            PricingEvent e = new PricingEvent();
            e.setOrderId(req.orderId());
            e.setCartTotal(req.orderValue());
            e.setDistanceKm(req.distanceKm() != null ? BigDecimal.valueOf(req.distanceKm()) : null);
            e.setWeightKg(req.weightKg() != null ? BigDecimal.valueOf(req.weightKg()) : null);
            e.setZoneId(zone != null ? zone.getId() : null);
            e.setDeliveryType(type != null ? type.getName() : req.deliveryType());
            // D-H5: qaysi qoida qo'llanilganini yozamiz — "eng yaxshi qoidalar",
            // "surcharge qo'llanilish soni" kabi statistikalar shundan hisoblanadi.
            e.setRuleId(rule != null ? rule.getId() : null);
            e.setRuleName(rule != null ? rule.getName() : null);
            // D-H4: qayerdan kelganini belgilaymiz (CREATE | QUOTE | INCENTIVE | LEGACY_QUOTE).
            e.setSource(source == null || source.isBlank() ? "CREATE" : source);
            e.setSurchargeNames(surchargeNames);
            e.setBaseFee(round(base));
            e.setDistanceFee(round(distanceFee));
            e.setWeightFee(round(weightFee));
            e.setSurchargeTotal(round(surchargeTotal));
            e.setDiscount(round(discount));
            e.setFinalFee(round(fee));
            e.setEstimatedCost(round(cost));
            e.setMargin(round(margin));
            e.setFreeDelivery(free);
            e.setThresholdAmount(threshold);
            eventRepository.save(e);
        } catch (Exception ex) {
            log.warn("Pricing event yozilmadi: {}", ex.getMessage());
        }
    }

    public PricingSettings settings() {
        return settingsRepository.findById(PricingSettings.DEFAULT_ID)
                .orElseGet(() -> settingsRepository.findAll().stream().findFirst()
                        .orElseGet(() -> settingsRepository.save(new PricingSettings())));
    }

    public PricingSettings saveSettings(PricingSettings s) {
        PricingSettings existing = settings();
        existing.setBaseCost(s.getBaseCost());
        existing.setPerKmCost(s.getPerKmCost());
        existing.setPerKgCost(s.getPerKgCost());
        if (s.getCurrency() != null && !s.getCurrency().isBlank()) existing.setCurrency(s.getCurrency());
        existing.setUpdatedAt(LocalDateTime.now());
        return settingsRepository.save(existing);
    }

    public LocalDate estimateDeliveryDate(PricingQuote quote) {
        int days = quote.estimatedDays() != null && quote.estimatedDays() > 0 ? quote.estimatedDays() : 1;
        return LocalDate.now().plusDays(days);
    }

    public boolean isFree(PricingQuote quote) {
        return quote.finalFee().signum() == 0 || quote.freeDelivery();
    }

    public boolean sameZone(DeliveryZone a, DeliveryZone b) {
        if (a == null || b == null) return Objects.equals(a, b);
        return Objects.equals(a.getId(), b.getId());
    }
}
