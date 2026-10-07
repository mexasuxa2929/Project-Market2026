package mexa.club.deliveryservice.pricing;

import lombok.RequiredArgsConstructor;
import mexa.club.deliveryservice.dto.ApiResponse;
import mexa.club.deliveryservice.entity.CartIncentiveRule;
import mexa.club.deliveryservice.entity.DeliverySurcharge;
import mexa.club.deliveryservice.entity.DeliveryType;
import mexa.club.deliveryservice.entity.FreeDeliveryRule;
import mexa.club.deliveryservice.entity.PricingAuditLog;
import mexa.club.deliveryservice.entity.PricingRule;
import mexa.club.deliveryservice.entity.PricingSettings;
import mexa.club.deliveryservice.entity.PricingVersion;
import mexa.club.deliveryservice.pricing.PricingDtos.PricingQuote;
import mexa.club.deliveryservice.pricing.PricingDtos.PricingRequest;
import mexa.club.deliveryservice.repository.CartIncentiveRuleRepository;
import mexa.club.deliveryservice.repository.DeliverySurchargeRepository;
import mexa.club.deliveryservice.repository.DeliveryTypeRepository;
import mexa.club.deliveryservice.repository.FreeDeliveryRuleRepository;
import mexa.club.deliveryservice.repository.PricingAuditLogRepository;
import mexa.club.deliveryservice.repository.PricingEventRepository;
import mexa.club.deliveryservice.repository.PricingRuleRepository;
import mexa.club.deliveryservice.repository.PricingVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin: Delivery Pricing & Cart Incentive Management.
 * Endpointlar: /api/admin/pricing/**
 */
@RestController
@RequestMapping("/api/admin/pricing")
@RequiredArgsConstructor
public class PricingAdminController {

    private final PricingRuleRepository ruleRepository;
    private final FreeDeliveryRuleRepository freeRepository;
    private final CartIncentiveRuleRepository incentiveRepository;
    private final DeliveryTypeRepository typeRepository;
    private final DeliverySurchargeRepository surchargeRepository;
    private final PricingVersionRepository versionRepository;
    private final PricingAuditLogRepository auditRepository;
    private final PricingEventRepository eventRepository;
    private final DeliveryPricingService pricingService;
    private final PricingAnalyticsService analyticsService;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    // ─── Pricing rules ─────────────────────────────────────────────────────

    @GetMapping("/rules")
    public ApiResponse<List<PricingRule>> rules() {
        return ApiResponse.ok(ruleRepository.findAllByOrderByPriorityDescCreatedAtDesc());
    }

    @PostMapping("/rules")
    public ApiResponse<PricingRule> createRule(@RequestBody PricingRule rule, Authentication auth) {
        validateRule(rule);
        rule.setId(null);
        if (rule.getCreatedAt() == null) rule.setCreatedAt(LocalDateTime.now());
        PricingRule saved = ruleRepository.save(rule);
        audit(auth, "CREATE", "PRICING_RULE", saved.getId(), null, json(saved), rule.getChangeReason());
        bumpVersion(auth, "Pricing rule yaratildi: " + saved.getName());
        return ApiResponse.ok(saved);
    }

    @PutMapping("/rules/{id}")
    public ApiResponse<PricingRule> updateRule(@PathVariable UUID id, @RequestBody PricingRule rule, Authentication auth) {
        validateRule(rule);
        PricingRule old = ruleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found"));
        String before = json(old);
        rule.setId(id);
        PricingRule saved = ruleRepository.save(rule);
        audit(auth, "UPDATE", "PRICING_RULE", id, before, json(saved), rule.getChangeReason());
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/rules/{id}")
    public ApiResponse<Map<String, Object>> deleteRule(@PathVariable UUID id, Authentication auth) {
        PricingRule old = ruleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found"));
        ruleRepository.delete(old);
        audit(auth, "DELETE", "PRICING_RULE", id, json(old), null, null);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    // ─── Free delivery ─────────────────────────────────────────────────────

    @GetMapping("/free-delivery")
    public ApiResponse<List<FreeDeliveryRule>> freeRules() {
        return ApiResponse.ok(freeRepository.findAllByOrderByMinOrderAsc());
    }

    @PostMapping("/free-delivery")
    public ApiResponse<FreeDeliveryRule> createFree(@RequestBody FreeDeliveryRule rule, Authentication auth) {
        validateFree(rule);
        rule.setId(null);
        FreeDeliveryRule saved = freeRepository.save(rule);
        audit(auth, "CREATE", "FREE_DELIVERY_RULE", saved.getId(), null, json(saved), null);
        bumpVersion(auth, "Free delivery qoidasi yaratildi: " + saved.getName());
        return ApiResponse.ok(saved);
    }

    @PutMapping("/free-delivery/{id}")
    public ApiResponse<FreeDeliveryRule> updateFree(@PathVariable UUID id, @RequestBody FreeDeliveryRule rule, Authentication auth) {
        validateFree(rule);
        FreeDeliveryRule old = freeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found"));
        String before = json(old);
        rule.setId(id);
        FreeDeliveryRule saved = freeRepository.save(rule);
        audit(auth, "UPDATE", "FREE_DELIVERY_RULE", id, before, json(saved), null);
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/free-delivery/{id}")
    public ApiResponse<Map<String, Object>> deleteFree(@PathVariable UUID id, Authentication auth) {
        FreeDeliveryRule old = freeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found"));
        freeRepository.delete(old);
        audit(auth, "DELETE", "FREE_DELIVERY_RULE", id, json(old), null, null);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    // ─── Cart incentives ───────────────────────────────────────────────────

    @GetMapping("/incentives")
    public ApiResponse<List<CartIncentiveRule>> incentives() {
        return ApiResponse.ok(incentiveRepository.findAllByOrderByThresholdAmountAsc());
    }

    @PostMapping("/incentives")
    public ApiResponse<CartIncentiveRule> createIncentive(@RequestBody CartIncentiveRule rule, Authentication auth) {
        validateIncentive(rule);
        rule.setId(null);
        CartIncentiveRule saved = incentiveRepository.save(rule);
        audit(auth, "CREATE", "CART_INCENTIVE_RULE", saved.getId(), null, json(saved), null);
        bumpVersion(auth, "Cart incentive yaratildi: " + saved.getName());
        return ApiResponse.ok(saved);
    }

    @PutMapping("/incentives/{id}")
    public ApiResponse<CartIncentiveRule> updateIncentive(@PathVariable UUID id, @RequestBody CartIncentiveRule rule, Authentication auth) {
        validateIncentive(rule);
        CartIncentiveRule old = incentiveRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found"));
        String before = json(old);
        rule.setId(id);
        CartIncentiveRule saved = incentiveRepository.save(rule);
        audit(auth, "UPDATE", "CART_INCENTIVE_RULE", id, before, json(saved), null);
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/incentives/{id}")
    public ApiResponse<Map<String, Object>> deleteIncentive(@PathVariable UUID id, Authentication auth) {
        CartIncentiveRule old = incentiveRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found"));
        incentiveRepository.delete(old);
        audit(auth, "DELETE", "CART_INCENTIVE_RULE", id, json(old), null, null);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    // ─── Delivery types ────────────────────────────────────────────────────

    @GetMapping("/types")
    public ApiResponse<List<DeliveryType>> types() {
        return ApiResponse.ok(typeRepository.findAll());
    }

    @PostMapping("/types")
    public ApiResponse<DeliveryType> createType(@RequestBody DeliveryType type, Authentication auth) {
        validateType(type);
        type.setId(null);
        DeliveryType saved = typeRepository.save(type);
        audit(auth, "CREATE", "DELIVERY_TYPE", saved.getId(), null, json(saved), null);
        return ApiResponse.ok(saved);
    }

    @PutMapping("/types/{id}")
    public ApiResponse<DeliveryType> updateType(@PathVariable UUID id, @RequestBody DeliveryType type, Authentication auth) {
        validateType(type);
        DeliveryType old = typeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type not found"));
        String before = json(old);
        type.setId(id);
        DeliveryType saved = typeRepository.save(type);
        audit(auth, "UPDATE", "DELIVERY_TYPE", id, before, json(saved), null);
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/types/{id}")
    public ApiResponse<Map<String, Object>> deleteType(@PathVariable UUID id, Authentication auth) {
        DeliveryType old = typeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type not found"));
        typeRepository.delete(old);
        audit(auth, "DELETE", "DELIVERY_TYPE", id, json(old), null, null);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    // ─── Surcharges ────────────────────────────────────────────────────────

    @GetMapping("/surcharges")
    public ApiResponse<List<DeliverySurcharge>> surcharges() {
        return ApiResponse.ok(surchargeRepository.findAllByOrderByPriorityDescCreatedAtAsc());
    }

    @PostMapping("/surcharges")
    public ApiResponse<DeliverySurcharge> createSurcharge(@RequestBody DeliverySurcharge s, Authentication auth) {
        validateSurcharge(s);
        s.setId(null);
        DeliverySurcharge saved = surchargeRepository.save(s);
        audit(auth, "CREATE", "SURCHARGE", saved.getId(), null, json(saved), null);
        return ApiResponse.ok(saved);
    }

    @PutMapping("/surcharges/{id}")
    public ApiResponse<DeliverySurcharge> updateSurcharge(@PathVariable UUID id, @RequestBody DeliverySurcharge s, Authentication auth) {
        validateSurcharge(s);
        DeliverySurcharge old = surchargeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Surcharge not found"));
        String before = json(old);
        s.setId(id);
        DeliverySurcharge saved = surchargeRepository.save(s);
        audit(auth, "UPDATE", "SURCHARGE", id, before, json(saved), null);
        return ApiResponse.ok(saved);
    }

    @DeleteMapping("/surcharges/{id}")
    public ApiResponse<Map<String, Object>> deleteSurcharge(@PathVariable UUID id, Authentication auth) {
        DeliverySurcharge old = surchargeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Surcharge not found"));
        surchargeRepository.delete(old);
        audit(auth, "DELETE", "SURCHARGE", id, json(old), null, null);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    // ─── Settings (real cost) ──────────────────────────────────────────────

    @GetMapping("/settings")
    public ApiResponse<PricingSettings> settings() {
        return ApiResponse.ok(pricingService.settings());
    }

    @PutMapping("/settings")
    public ApiResponse<PricingSettings> saveSettings(@RequestBody PricingSettings settings, Authentication auth) {
        validateSettings(settings);
        String before = json(pricingService.settings());
        PricingSettings saved = pricingService.saveSettings(settings);
        audit(auth, "UPDATE", "PRICING_SETTINGS", null, before, json(saved), null);
        return ApiResponse.ok(saved);
    }

    // ─── Versions + audit ──────────────────────────────────────────────────

    @GetMapping("/versions")
    public ApiResponse<List<PricingVersion>> versions() {
        return ApiResponse.ok(versionRepository.findAllByOrderByVersionDesc());
    }

    @PostMapping("/versions")
    public ApiResponse<PricingVersion> createVersion(@RequestBody(required = false) PricingVersion req, Authentication auth) {
        PricingVersion last = versionRepository.findTopByOrderByVersionDesc();
        int next = last == null ? 1 : last.getVersion() + 1;
        if (last != null && "ACTIVE".equals(last.getStatus())) {
            last.setStatus("ARCHIVED");
            last.setEffectiveUntil(LocalDateTime.now());
            versionRepository.save(last);
        }
        PricingVersion v = new PricingVersion();
        v.setVersion(next);
        v.setStatus("ACTIVE");
        v.setCreatedBy(userName(auth));
        v.setEffectiveFrom(LocalDateTime.now());
        v.setChangeReason(req != null ? req.getChangeReason() : null);
        PricingVersion saved = versionRepository.save(v);
        audit(auth, "CREATE_VERSION", "PRICING_VERSION", saved.getId(), null, json(saved), saved.getChangeReason());
        return ApiResponse.ok(saved);
    }

    @GetMapping("/audit")
    public ApiResponse<List<PricingAuditLog>> auditLog() {
        List<PricingAuditLog> list = new ArrayList<>(auditRepository.findAll());
        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        if (list.size() > 200) list = list.subList(0, 200);
        return ApiResponse.ok(list);
    }

    // ─── Simulator ─────────────────────────────────────────────────────────

    public record SimulatorRequest(
            UUID zoneId, String region, String district,
            Double distanceKm, Double weightKg, BigDecimal orderValue,
            String deliveryType, LocalDateTime time) {}

    @PostMapping("/simulator")
    public ApiResponse<PricingQuote> simulate(@RequestBody SimulatorRequest req) {
        PricingQuote quote = pricingService.calculate(new PricingRequest(
                req.zoneId(), req.region(), req.district(),
                req.distanceKm(), req.weightKg(), req.orderValue(),
                req.deliveryType(), req.time() != null ? req.time() : LocalDateTime.now(),
                null, false));
        return ApiResponse.ok(quote);
    }

    /** Rule preview: human-readable IF/THEN ko'rinishi. */
    @GetMapping("/preview")
    public ApiResponse<Map<String, Object>> preview(@RequestParam(required = false) UUID ruleId) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (ruleId != null) {
            PricingRule r = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found"));
            out.put("if", "Order vaqtida faol qoida: " + r.getName());
            out.put("formula", "Base " + r.getBaseFee() + " + (Distance × " + r.getPricePerKm()
                    + ") + (Weight × " + r.getPricePerKg() + ")");
            out.put("clamp", "min=" + r.getMinFee() + ", max=" + (r.getMaxFee() != null ? r.getMaxFee() : "—"));
            out.put("then", "Final Fee = min(max, max(min, Base + Distance fee + Weight fee)) — surcharges va free delivery keyin qo'llanadi");
            out.put("zoneId", r.getZoneId());
            out.put("deliveryType", r.getDeliveryType());
            out.put("active", r.getActive());
            out.put("priority", r.getPriority());
            out.put("window", (r.getStartAt() != null ? r.getStartAt() : "—") + " → " + (r.getEndAt() != null ? r.getEndAt() : "—"));
        }
        return ApiResponse.ok(out);
    }

    // ─── Analytics ─────────────────────────────────────────────────────────

    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> analytics(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(analyticsService.overview(days));
    }

    // ─── Validatsiya ──────────────────────────────────────────────────────

    private static void validateRule(PricingRule rule) {
        validateNonNegative(rule.getBaseFee(), "baseFee");
        validateNonNegative(rule.getPricePerKm(), "pricePerKm");
        validateNonNegative(rule.getPricePerKg(), "pricePerKg");
        validateNonNegative(rule.getMinFee(), "minFee");
        if (rule.getMaxFee() != null && rule.getMaxFee().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxFee 0 dan katta bo'lishi kerak");
        }
        validateWindow(rule.getStartAt(), rule.getEndAt());
    }

    private static void validateFree(FreeDeliveryRule rule) {
        validateNonNegative(rule.getMinOrder(), "minOrder");
        validateWindow(rule.getStartAt(), rule.getEndAt());
    }

    private static void validateIncentive(CartIncentiveRule rule) {
        if (rule.getThresholdAmount() == null || rule.getThresholdAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "thresholdAmount 0 dan katta bo'lishi kerak");
        }
        validateNonNegative(rule.getRewardValue(), "rewardValue");
        if (rule.getRewardValue() != null && "PERCENT".equalsIgnoreCase(rule.getRewardType())
                && rule.getRewardValue().compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PERCENT rewardValue 100 dan katta bo'lishi mumkin emas");
        }
        validateWindow(rule.getStartAt(), rule.getEndAt());
    }

    private static void validateType(DeliveryType t) {
        if (t.getName() == null || t.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name bo'sh bo'lmasligi kerak");
        }
        validateNonNegative(t.getAdditionalFee(), "additionalFee");
    }

    private static void validateSurcharge(DeliverySurcharge s) {
        validateNonNegative(s.getFee(), "fee");
        validateNonNegative(s.getConditionValue(), "conditionValue");
        if ("PERCENT".equalsIgnoreCase(s.getFeeType()) && s.getFee() != null
                && s.getFee().compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "PERCENT fee 100 dan katta bo'lishi mumkin emas");
        }
    }

    private static void validateSettings(PricingSettings s) {
        validateNonNegative(s.getBaseCost(), "baseCost");
        validateNonNegative(s.getPerKmCost(), "perKmCost");
        validateNonNegative(s.getPerKgCost(), "perKgCost");
    }

    private static void validateNonNegative(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " manfiy bo'lishi mumkin emas");
        }
    }

    private static void validateWindow(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startAt endAt dan keyin bo'lishi mumkin emas");
        }
    }

    // ─── Yordamchilar ──────────────────────────────────────────────────────

    private void audit(Authentication auth, String action, String entityType, UUID entityId,
                       String oldValue, String newValue, String reason) {
        try {
            PricingAuditLog log = new PricingAuditLog();
            log.setAdminName(userName(auth));
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setOldValue(oldValue);
            log.setNewValue(newValue);
            log.setReason(reason);
            auditRepository.save(log);
        } catch (Exception ignored) {
        }
    }

    private void bumpVersion(Authentication auth, String reason) {
        PricingVersion last = versionRepository.findTopByOrderByVersionDesc();
        int next = last == null ? 1 : last.getVersion() + 1;
        if (last != null && "ACTIVE".equals(last.getStatus())) {
            last.setStatus("ARCHIVED");
            last.setEffectiveUntil(LocalDateTime.now());
            versionRepository.save(last);
        }
        PricingVersion v = new PricingVersion();
        v.setVersion(next);
        v.setStatus("ACTIVE");
        v.setCreatedBy(userName(auth));
        v.setEffectiveFrom(LocalDateTime.now());
        v.setChangeReason(reason);
        versionRepository.save(v);
    }

    private static String userName(Authentication auth) {
        if (auth == null) return "admin";
        Object p = auth.getPrincipal();
        if (p instanceof mexa.club.deliveryservice.security.JwtUserPrincipal jp && jp.username() != null) {
            return jp.username();
        }
        String name = auth.getName();
        return name == null || name.isBlank() ? "admin" : name;
    }

    private String json(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }
}
