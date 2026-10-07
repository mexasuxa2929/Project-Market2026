package mexa.club.deliveryservice.pricing;

import mexa.club.deliveryservice.entity.CartIncentiveRule;
import mexa.club.deliveryservice.entity.DeliveryZone;
import mexa.club.deliveryservice.entity.FreeDeliveryRule;
import mexa.club.deliveryservice.entity.PricingEvent;
import mexa.club.deliveryservice.entity.PricingRule;
import mexa.club.deliveryservice.entity.PricingSettings;
import mexa.club.deliveryservice.pricing.PricingDtos.PricingQuote;
import mexa.club.deliveryservice.pricing.PricingDtos.PricingRequest;
import mexa.club.deliveryservice.repository.CartIncentiveRuleRepository;
import mexa.club.deliveryservice.repository.DeliverySurchargeRepository;
import mexa.club.deliveryservice.repository.DeliveryTypeRepository;
import mexa.club.deliveryservice.repository.DeliveryZoneRepository;
import mexa.club.deliveryservice.repository.FreeDeliveryRuleRepository;
import mexa.club.deliveryservice.repository.PricingEventRepository;
import mexa.club.deliveryservice.repository.PricingRuleRepository;
import mexa.club.deliveryservice.repository.PricingSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DeliveryPricingServiceTest {

    private static final LocalDateTime TIME = LocalDateTime.of(2026, 6, 15, 12, 0);

    private DeliveryZoneRepository zoneRepository;
    private PricingRuleRepository ruleRepository;
    private FreeDeliveryRuleRepository freeDeliveryRuleRepository;
    private CartIncentiveRuleRepository incentiveRuleRepository;
    private DeliveryTypeRepository deliveryTypeRepository;
    private DeliverySurchargeRepository surchargeRepository;
    private PricingSettingsRepository settingsRepository;
    private PricingEventRepository eventRepository;

    private DeliveryPricingService service;

    @BeforeEach
    void setUp() {
        zoneRepository = mock(DeliveryZoneRepository.class);
        ruleRepository = mock(PricingRuleRepository.class);
        freeDeliveryRuleRepository = mock(FreeDeliveryRuleRepository.class);
        incentiveRuleRepository = mock(CartIncentiveRuleRepository.class);
        deliveryTypeRepository = mock(DeliveryTypeRepository.class);
        surchargeRepository = mock(DeliverySurchargeRepository.class);
        settingsRepository = mock(PricingSettingsRepository.class);
        eventRepository = mock(PricingEventRepository.class);

        lenient().when(ruleRepository.findAllByOrderByPriorityDescCreatedAtDesc())
                .thenReturn(new ArrayList<>());
        lenient().when(freeDeliveryRuleRepository.findAllByOrderByMinOrderAsc())
                .thenReturn(new ArrayList<>());
        lenient().when(incentiveRuleRepository.findAllByOrderByThresholdAmountAsc())
                .thenReturn(new ArrayList<>());
        lenient().when(surchargeRepository.findAllByOrderByPriorityDescCreatedAtAsc())
                .thenReturn(new ArrayList<>());

        PricingSettings settings = new PricingSettings();
        settings.setBaseCost(new BigDecimal("1000"));
        settings.setPerKmCost(new BigDecimal("100"));
        settings.setPerKgCost(new BigDecimal("10"));
        lenient().when(settingsRepository.findById(PricingSettings.DEFAULT_ID))
                .thenReturn(Optional.of(settings));
        lenient().when(settingsRepository.findAll()).thenReturn(List.of(settings));
        lenient().when(eventRepository.save(any(PricingEvent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service = new DeliveryPricingService(zoneRepository, ruleRepository,
                freeDeliveryRuleRepository, incentiveRuleRepository, deliveryTypeRepository,
                surchargeRepository, settingsRepository, eventRepository);
    }

    // ─── 1. Default tarif ─────────────────────────────────────────────────

    @Test
    void defaultTariff_whenNoRuleAndNoZone() {
        PricingQuote quote = service.calculate(request(null, 5.0, 0.0, null));

        assertEquals(new BigDecimal("8000"), quote.baseFee());
        assertEquals(new BigDecimal("10000"), quote.distanceFee());
        assertEquals(new BigDecimal("0"), quote.weightFee());
        assertEquals(new BigDecimal("18000"), quote.finalFee());
        assertEquals(new BigDecimal("0"), quote.discount());
        assertEquals(new BigDecimal("1500"), quote.estimatedCost());
        assertEquals(new BigDecimal("16500"), quote.margin());
        assertFalse(quote.freeDelivery());
        assertEquals("Default tarif", quote.appliedRule());
    }

    // ─── 2. Zona tarifi ───────────────────────────────────────────────────

    @Test
    void zoneFeeApplies_whenNoRule() {
        UUID zoneId = UUID.randomUUID();
        givenZone(zoneId, "12000", "1000", "500");

        PricingQuote quote = service.calculate(request(zoneId, 3.0, 2.0, null));

        assertEquals(new BigDecimal("12000"), quote.baseFee());
        assertEquals(new BigDecimal("3000"), quote.distanceFee());
        assertEquals(new BigDecimal("1000"), quote.weightFee());
        assertEquals(new BigDecimal("16000"), quote.finalFee());
        assertEquals("Zona: Toshkent/Chilonzor", quote.appliedRule());
    }

    // ─── 3. REGRESSION: 0 = sozlanmagan ───────────────────────────────────

    @Test
    void regression_zeroFeeRuleIsIgnored_zoneOrDefaultUsed() {
        PricingRule zero = rule("Nol qoida", "0", "0", "0", "0", null);
        lenient().when(ruleRepository.findAllByOrderByPriorityDescCreatedAtDesc())
                .thenReturn(new ArrayList<>(List.of(zero)));

        UUID zoneId = UUID.randomUUID();
        givenZone(zoneId, "12000", "0", "0");

        PricingQuote withZone = service.calculate(request(zoneId, 0.0, 0.0, null));
        assertEquals("Zona: Toshkent/Chilonzor", withZone.appliedRule());
        assertTrue(withZone.finalFee().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("12000"), withZone.finalFee());

        PricingQuote withDefaults = service.calculate(request(null, 0.0, 0.0, null));
        assertEquals("Default tarif", withDefaults.appliedRule());
        assertTrue(withDefaults.finalFee().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("8000"), withDefaults.finalFee());
    }

    // ─── 4. Qisman sozlangan qoida ────────────────────────────────────────

    @Test
    void ruleBaseFeeOverridesZone_otherFieldsFallBack() {
        UUID zoneId = UUID.randomUUID();
        givenZone(zoneId, "12000", "1500", "0");
        lenient().when(ruleRepository.findAllByOrderByPriorityDescCreatedAtDesc())
                .thenReturn(new ArrayList<>(List.of(rule("Base 10000", "10000", "0", "0", "0", null))));

        PricingQuote quote = service.calculate(request(zoneId, 4.0, 3.0, null));

        assertEquals(new BigDecimal("10000"), quote.baseFee());
        assertEquals(new BigDecimal("6000"), quote.distanceFee());
        assertEquals(new BigDecimal("0"), quote.weightFee());
        assertEquals(new BigDecimal("16000"), quote.finalFee());
        assertEquals("Base 10000", quote.appliedRule());
    }

    // ─── 5. Min klamp ─────────────────────────────────────────────────────

    @Test
    void minFeeClamp_raisesTooSmallFee() {
        lenient().when(ruleRepository.findAllByOrderByPriorityDescCreatedAtDesc())
                .thenReturn(new ArrayList<>(List.of(rule("Kam haq", "5000", "0", "0", "9000", null))));

        PricingQuote quote = service.calculate(request(null, 0.0, 0.0, null));

        assertEquals(new BigDecimal("5000"), quote.baseFee());
        assertEquals(new BigDecimal("9000"), quote.finalFee());
    }

    // ─── 6. Max klamp ─────────────────────────────────────────────────────

    @Test
    void maxFeeClamp_lowersTooLargeFee() {
        lenient().when(ruleRepository.findAllByOrderByPriorityDescCreatedAtDesc())
                .thenReturn(new ArrayList<>(List.of(rule("Cheklov", "50000", "0", "0", "0", "30000"))));

        PricingQuote quote = service.calculate(request(null, 0.0, 0.0, null));

        assertEquals(new BigDecimal("50000"), quote.baseFee());
        assertEquals(new BigDecimal("30000"), quote.finalFee());
    }

    // ─── 7. Masofa + og'irlik ─────────────────────────────────────────────

    @Test
    void distanceAndWeight_addToFee() {
        UUID zoneId = UUID.randomUUID();
        givenZone(zoneId, "8000", "2000", "500");

        PricingQuote quote = service.calculate(request(zoneId, 10.0, 5.0, null));

        assertEquals(new BigDecimal("8000"), quote.baseFee());
        assertEquals(new BigDecimal("20000"), quote.distanceFee());
        assertEquals(new BigDecimal("2500"), quote.weightFee());
        assertEquals(new BigDecimal("30500"), quote.finalFee());
    }

    // ─── 8. Bepul yetkazish ───────────────────────────────────────────────

    @Test
    void freeDelivery_orderAboveThreshold_feeZero() {
        lenient().when(freeDeliveryRuleRepository.findAllByOrderByMinOrderAsc())
                .thenReturn(new ArrayList<>(List.of(freeRule("Bepul 50k", "50000", "0"))));

        PricingQuote quote = service.calculate(request(null, 0.0, 0.0, new BigDecimal("60000")));

        assertEquals(new BigDecimal("0"), quote.finalFee());
        assertTrue(quote.freeDelivery());
        assertEquals(new BigDecimal("8000"), quote.discount());
    }

    // ─── 9. REGRESSION C2: chegiradagi haq beforeDiscount dan oshmaydi ────

    @Test
    void regression_freeRuleFeeAboveBeforeDiscount_neverExceeds() {
        lenient().when(freeDeliveryRuleRepository.findAllByOrderByMinOrderAsc())
                .thenReturn(new ArrayList<>(List.of(freeRule("Bepul 10k", "10000", "999999"))));

        PricingQuote quote = service.calculate(request(null, 0.0, 0.0, new BigDecimal("20000")));

        assertEquals(new BigDecimal("8000"), quote.finalFee());
        assertTrue(quote.finalFee().compareTo(new BigDecimal("8000")) <= 0);
        assertTrue(quote.freeDelivery());
        assertEquals(new BigDecimal("0"), quote.discount());
    }

    @Test
    void freeRuleFeePartiallyClampsBeforeDiscount() {
        lenient().when(freeDeliveryRuleRepository.findAllByOrderByMinOrderAsc())
                .thenReturn(new ArrayList<>(List.of(freeRule("Bepul 10k", "10000", "5000"))));

        PricingQuote quote = service.calculate(request(null, 0.0, 0.0, new BigDecimal("20000")));

        assertEquals(new BigDecimal("5000"), quote.finalFee());
        assertEquals(new BigDecimal("3000"), quote.discount());
        assertTrue(quote.freeDelivery());
    }

    // ─── 10-12. Cart incentive ────────────────────────────────────────────

    @Test
    void incentiveAmount_reducesFee() {
        givenIncentive("10000", "AMOUNT", "3000");

        PricingQuote quote = service.calculate(request(null, 0.0, 0.0, new BigDecimal("15000")));

        assertEquals(new BigDecimal("5000"), quote.finalFee());
        assertEquals(new BigDecimal("3000"), quote.discount());
        assertFalse(quote.freeDelivery());
    }

    @Test
    void incentivePercent_reducesFee() {
        givenIncentive("10000", "PERCENT", "50");

        PricingQuote quote = service.calculate(request(null, 0.0, 0.0, new BigDecimal("15000")));

        assertEquals(new BigDecimal("4000"), quote.finalFee());
        assertEquals(new BigDecimal("4000"), quote.discount());
        assertFalse(quote.freeDelivery());
    }

    @Test
    void incentiveFree_feeZero() {
        givenIncentive("10000", "FREE", "0");

        PricingQuote quote = service.calculate(request(null, 0.0, 0.0, new BigDecimal("15000")));

        assertEquals(new BigDecimal("0"), quote.finalFee());
        assertTrue(quote.freeDelivery());
        assertEquals(new BigDecimal("8000"), quote.discount());
    }

    // ─── 13. Manfiy rewardValue ───────────────────────────────────────────

    @Test
    void negativeRewardValue_noNegativeDiscount() {
        givenIncentive("10000", "AMOUNT", "-5000");

        PricingQuote amount = service.calculate(request(null, 0.0, 0.0, new BigDecimal("15000")));
        assertEquals(new BigDecimal("8000"), amount.finalFee());
        assertTrue(amount.discount().signum() >= 0);
        assertEquals(new BigDecimal("0"), amount.discount());

        givenIncentive("10000", "FREE", "-700");
        PricingQuote free = service.calculate(request(null, 0.0, 0.0, new BigDecimal("15000")));
        assertEquals(new BigDecimal("0"), free.finalFee());
        assertTrue(free.discount().signum() >= 0);
        assertEquals(new BigDecimal("8000"), free.discount());
    }

    // ─── 14. Rounding (HALF_UP, scale 0) ──────────────────────────────────

    @Test
    void rounding_usesHalfUpScaleZero() {
        UUID zoneId = UUID.randomUUID();
        givenZone(zoneId, "8000", "1001", "0");

        PricingQuote quote = service.calculate(request(zoneId, 0.5, null, null));

        assertEquals(new BigDecimal("501"), quote.distanceFee());
        assertEquals(new BigDecimal("8501"), quote.finalFee());
    }

    // ─── 15. recordEvent yo'li ────────────────────────────────────────────

    @Test
    void recordEvent_savedOnlyWhenRequested() {
        service.calculate(request(null, 0.0, 0.0, null));
        verify(eventRepository, never()).save(any(PricingEvent.class));

        service.calculate(new PricingRequest(null, null, null, 0.0, null, null, null,
                TIME, UUID.randomUUID(), true));
        verify(eventRepository, times(1)).save(any(PricingEvent.class));
    }

    @Test
    void quote_withoutRecordEvent_doesNotTouchEventRepo() {
        PricingQuote quote = service.quote("Toshkent", "Chilonzor", 5.0, 1.0, new BigDecimal("20000"));

        assertEquals(new BigDecimal("8000"), quote.baseFee());
        assertNull(quote.incentive());
        verify(eventRepository, never()).save(any(PricingEvent.class));
    }

    // ─── Yordamchilar ─────────────────────────────────────────────────────

    private static PricingRequest request(UUID zoneId, Double km, Double kg, BigDecimal orderValue) {
        return new PricingRequest(zoneId, null, null, km, kg, orderValue, null, TIME, null, false);
    }

    private void givenZone(UUID id, String fee, String perKm, String perKg) {
        DeliveryZone zone = new DeliveryZone();
        zone.setId(id);
        zone.setRegion("Toshkent");
        zone.setDistrict("Chilonzor");
        zone.setFee(new BigDecimal(fee));
        zone.setPerKmFee(new BigDecimal(perKm));
        zone.setPerKgFee(new BigDecimal(perKg));
        zone.setEstimatedDays(1);
        lenient().when(zoneRepository.findById(id)).thenReturn(Optional.of(zone));
    }

    private static PricingRule rule(String name, String baseFee, String perKm,
                                    String perKg, String minFee, String maxFee) {
        PricingRule r = new PricingRule();
        r.setName(name);
        r.setBaseFee(new BigDecimal(baseFee));
        r.setPricePerKm(new BigDecimal(perKm));
        r.setPricePerKg(new BigDecimal(perKg));
        r.setMinFee(new BigDecimal(minFee));
        r.setMaxFee(maxFee == null ? null : new BigDecimal(maxFee));
        r.setActive(true);
        return r;
    }

    private static FreeDeliveryRule freeRule(String name, String minOrder, String deliveryFee) {
        FreeDeliveryRule r = new FreeDeliveryRule();
        r.setName(name);
        r.setMinOrder(new BigDecimal(minOrder));
        r.setDeliveryFee(new BigDecimal(deliveryFee));
        r.setActive(true);
        return r;
    }

    private void givenIncentive(String threshold, String rewardType, String rewardValue) {
        CartIncentiveRule r = new CartIncentiveRule();
        r.setName("Incentive " + rewardType);
        r.setThresholdAmount(new BigDecimal(threshold));
        r.setRewardType(rewardType);
        r.setRewardValue(new BigDecimal(rewardValue));
        r.setActive(true);
        lenient().when(incentiveRuleRepository.findAllByOrderByThresholdAmountAsc())
                .thenReturn(new ArrayList<>(List.of(r)));
    }
}
