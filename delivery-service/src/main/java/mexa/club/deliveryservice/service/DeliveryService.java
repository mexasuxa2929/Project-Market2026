package mexa.club.deliveryservice.service;

import jakarta.persistence.criteria.Predicate;
import mexa.club.deliveryservice.client.OrderInternalClient;
import mexa.club.deliveryservice.client.payload.DeliveryFailedRequest;
import mexa.club.deliveryservice.dto.AssignCourierRequest;
import mexa.club.deliveryservice.dto.CourierRequest;
import mexa.club.deliveryservice.dto.CourierStatsResponse;
import mexa.club.deliveryservice.dto.CourierWarehouseStatsResponse;
import mexa.club.deliveryservice.dto.CreateDeliveryRequest;
import mexa.club.deliveryservice.dto.DeliveryFeeQuoteResponse;
import mexa.club.deliveryservice.dto.DeliveryFeeResponse;
import mexa.club.deliveryservice.dto.DeliveryResponse;
import mexa.club.deliveryservice.dto.UpdateDeliveryStatusRequest;
import mexa.club.deliveryservice.entity.Courier;
import mexa.club.deliveryservice.entity.Delivery;
import mexa.club.deliveryservice.entity.DeliveryStatus;
import mexa.club.deliveryservice.entity.DeliveryTrackingEvent;
import mexa.club.deliveryservice.entity.DeliveryZone;
import mexa.club.deliveryservice.exception.DeliveryException;
import mexa.club.deliveryservice.repository.CourierRepository;
import mexa.club.deliveryservice.repository.DeliveryRepository;
import mexa.club.deliveryservice.repository.DeliveryTrackingEventRepository;
import mexa.club.deliveryservice.repository.DeliveryZoneRepository;
import mexa.club.deliveryservice.dto.DeliveryNoteRequest;
import mexa.club.deliveryservice.dto.DeliveryFeeUpdateRequest;
import mexa.club.deliveryservice.dto.DeliveryZoneRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class DeliveryService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeliveryService.class);

    private final DeliveryRepository deliveryRepository;
    private final DeliveryTrackingEventRepository eventRepository;
    private final CourierRepository courierRepository;
    private final DeliveryZoneRepository zoneRepository;
    private final mexa.club.deliveryservice.repository.DeliveryZoneAuditRepository auditRepository;
    private final mexa.club.deliveryservice.repository.CourierActionAuditRepository courierAuditRepository;
    private final DeliveryMapper mapper;
    private final DeliveryNotificationService deliveryNotificationService;
    private final OrderInternalClient orderInternalClient;
    private final mexa.club.deliveryservice.pricing.DeliveryPricingService pricingService;

    public DeliveryService(
            DeliveryRepository deliveryRepository,
            DeliveryTrackingEventRepository eventRepository,
            CourierRepository courierRepository,
            DeliveryZoneRepository zoneRepository,
            mexa.club.deliveryservice.repository.DeliveryZoneAuditRepository auditRepository,
            mexa.club.deliveryservice.repository.CourierActionAuditRepository courierAuditRepository,
            DeliveryMapper mapper,
            DeliveryNotificationService deliveryNotificationService,
            OrderInternalClient orderInternalClient,
            mexa.club.deliveryservice.pricing.DeliveryPricingService pricingService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.eventRepository = eventRepository;
        this.courierRepository = courierRepository;
        this.zoneRepository = zoneRepository;
        this.auditRepository = auditRepository;
        this.courierAuditRepository = courierAuditRepository;
        this.mapper = mapper;
        this.deliveryNotificationService = deliveryNotificationService;
        this.orderInternalClient = orderInternalClient;
        this.pricingService = pricingService;
    }

    /**
     * Yetkazish DELIVERED bo'lganda order-service'ga xabar beradi.
     * Xatolik bo'lsa faqat log yoziladi — delivery oqimi hech qachon uzilmaydi.
     */
    private void notifyOrderDelivered(Delivery d) {
        if (d.getOrderId() == null) {
            return;
        }
        try {
            orderInternalClient.markDelivered(d.getOrderId());
        } catch (Exception ex) {
            log.warn("Order status sync failed for delivery {}: {}", d.getId(), ex.getMessage());
        }
    }

    /**
     * Yetkazib bo'lmaganda (FAILED / RETURNED) order-service'ga xabar beradi.
     * Xatolik bo'lsa faqat log yoziladi — delivery oqimi hech qachon uzilmaydi.
     */
    private void notifyOrderDeliveryFailed(Delivery d, String reason) {
        if (d.getOrderId() == null) {
            return;
        }
        try {
            orderInternalClient.deliveryFailed(d.getOrderId(), new DeliveryFailedRequest(reason));
        } catch (Exception ex) {
            log.warn("Order status sync failed for delivery {}: {}", d.getId(), ex.getMessage());
        }
    }

    @Transactional
    public DeliveryResponse create(CreateDeliveryRequest request) {
        // Idempotency: order uchun delivery mavjud bo'lsa yangi yaratilmaydi (qayta CONFIRMED hollari).
        Delivery existing = deliveryRepository.findByOrderId(request.orderId()).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }
        // Pricing engine: admin qoidalari (formula + zona + surcharge + free delivery + incentive)
        // orqali fee avtomatik hisoblanadi va analytics event yoziladi.
        mexa.club.deliveryservice.pricing.PricingDtos.PricingQuote quote = pricingService.calculate(
                new mexa.club.deliveryservice.pricing.PricingDtos.PricingRequest(
                        null, request.region(), request.district(),
                        request.distanceKm(), request.weightKg(), request.orderSubtotal(),
                        request.deliveryType(), java.time.LocalDateTime.now(),
                        request.orderId(), true));
        Delivery d = new Delivery();
        d.setOrderId(request.orderId());
        d.setFromWarehouseId(request.warehouseId());
        d.setDeliveryAddress(request.address());
        d.setRecipientName(request.recipientName());
        d.setRecipientPhone(request.recipientPhone());
        d.setRegion(request.region());
        d.setDistrict(request.district());
        d.setDeliveryFee(quote.finalFee());
        d.setEstimatedDelivery(LocalDate.now().plusDays(
                quote.estimatedDays() != null && quote.estimatedDays() > 0 ? quote.estimatedDays() : 1));
        d.setStatus(DeliveryStatus.PENDING);
        d.setTrackingCode(nextTrackingCode());
        Delivery saved = deliveryRepository.save(d);
        logEvent(saved.getId(), saved.getStatus(), null, "Delivery created");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> all(int page, int size) {
        return deliveryRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getByTracking(String code) {
        Delivery d = deliveryRepository.findByTrackingCode(code).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Tracking code not found"));
        return toResponse(d);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getByOrderId(UUID orderId) {
        Delivery d = deliveryRepository.findByOrderId(orderId).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Order delivery not found"));
        return toResponse(d);
    }

    @Transactional
    public DeliveryResponse assign(UUID id, AssignCourierRequest request) {
        Delivery d = getDelivery(id);
        Courier courier = courierRepository.findById(request.courierId()).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "COURIER_NOT_FOUND", "Courier not found"));
        if (!courier.isActive()) {
            throw new DeliveryException(HttpStatus.BAD_REQUEST, "COURIER_INACTIVE", "Courier is inactive");
        }
        if (d.getCourierId() != null && !d.getCourierId().equals(courier.getId())) {
            Courier prev = courierRepository.findById(d.getCourierId()).orElse(null);
            if (prev != null && prev.getCurrentDeliveries() > 0) {
                prev.setCurrentDeliveries(prev.getCurrentDeliveries() - 1);
                courierRepository.save(prev);
            }
        }
        d.setCourierId(courier.getId());
        d.setStatus(DeliveryStatus.ASSIGNED);
        courier.setCurrentDeliveries(courier.getCurrentDeliveries() + 1);
        courierRepository.save(courier);
        deliveryRepository.save(d);
        auditCourier(courier, "ASSIGNED_DELIVERY", d.getId(), d.getOrderId(), null, null);
        logEvent(d.getId(), d.getStatus(), null, "Courier assigned: " + courier.getName());
        deliveryNotificationService.notifyAssigned(d, courier.getName());
        return toResponse(d);
    }

    @Transactional
    public DeliveryResponse updateCourierStatus(UUID deliveryId, UUID courierUserId, UpdateDeliveryStatusRequest request) {
        Delivery d = getCourierDelivery(deliveryId, courierUserId);
        d.setStatus(request.status());
        if (request.status() == DeliveryStatus.DELIVERED) {
            d.setActualDelivery(LocalDateTime.now());
            decreaseCourierLoad(d.getCourierId());
        }
        deliveryRepository.save(d);
        logEvent(d.getId(), request.status(), request.location(), "Status updated by courier");
        if (request.status() == DeliveryStatus.DELIVERED) {
            deliveryNotificationService.notifyCompleted(d);
            notifyOrderDelivered(d);
        } else {
            deliveryNotificationService.notifyStatusUpdated(d, request.status());
        }
        return toResponse(d);
    }

    @Transactional
    public DeliveryResponse complete(UUID deliveryId, UUID courierUserId) {
        Delivery d = getCourierDelivery(deliveryId, courierUserId);
        d.setStatus(DeliveryStatus.DELIVERED);
        d.setActualDelivery(LocalDateTime.now());
        deliveryRepository.save(d);
        decreaseCourierLoad(d.getCourierId());
        logEvent(d.getId(), DeliveryStatus.DELIVERED, null, "Delivery completed");
        courierRepository.findById(d.getCourierId()).ifPresent(c ->
                auditCourier(c, "COMPLETED_DELIVERY", d.getId(), d.getOrderId(), null, null));
        deliveryNotificationService.notifyCompleted(d);
        notifyOrderDelivered(d);
        return toResponse(d);
    }

    @Transactional
    public DeliveryResponse fail(UUID deliveryId, UUID courierUserId, String reason) {
        Delivery d = getCourierDelivery(deliveryId, courierUserId);
        d.setStatus(DeliveryStatus.FAILED);
        d.setFailReason(reason);
        deliveryRepository.save(d);
        decreaseCourierLoad(d.getCourierId());
        logEvent(d.getId(), DeliveryStatus.FAILED, null, "Delivery failed: " + reason);
        courierRepository.findById(d.getCourierId()).ifPresent(c ->
                auditCourier(c, "FAILED_DELIVERY", d.getId(), d.getOrderId(), null, reason));
        deliveryNotificationService.notifyFailed(d, reason);
        notifyOrderDeliveryFailed(d, "Delivery failed: " + reason);
        return toResponse(d);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> courierDeliveries(UUID courierUserId) {
        Courier courier = courierRepository.findByUserId(courierUserId).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "COURIER_NOT_FOUND", "Courier not found"));
        return deliveryRepository.findByCourierIdOrderByCreatedAtDesc(courier.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<Courier> couriers() {
        return courierRepository.findAll();
    }

    @Transactional
    public Courier createCourier(CourierRequest request) {
        Courier courier = new Courier();
        courier.setUserId(request.userId());
        courier.setName(request.name());
        courier.setPhone(request.phone());
        courier.setRegion(request.region());
        courier.setActive(request.active());
        courier.setWarehouseId(request.warehouseId());
        Courier saved = courierRepository.save(courier);
        auditCourier(saved, "CREATED", null, null, saved.getWarehouseId(), null);
        return saved;
    }

    @Transactional
    public Courier updateCourier(UUID id, CourierRequest request) {
        Courier courier = courierRepository.findById(id).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "COURIER_NOT_FOUND", "Courier not found"));
        courier.setUserId(request.userId());
        courier.setName(request.name());
        courier.setPhone(request.phone());
        courier.setRegion(request.region());
        courier.setActive(request.active());
        if (request.warehouseId() != null) {
            courier.setWarehouseId(request.warehouseId());
        }
        Courier saved = courierRepository.save(courier);
        auditCourier(saved, "UPDATED", null, null, saved.getWarehouseId(), null);
        return saved;
    }

    /** Shu omborga biriktirilgan kuryerlar — boshqa omborlarga chalkashmaydi. */
    @Transactional(readOnly = true)
    public List<Courier> warehouseCouriers(UUID warehouseId) {
        return courierRepository.findByWarehouseId(warehouseId);
    }

    /** Biriktirilmagan kuryerlar (ombor tanlash ro'yxati uchun). */
    @Transactional(readOnly = true)
    public List<Courier> unassignedCouriers() {
        return courierRepository.findByWarehouseIdIsNull();
    }

    /**
     * Kuryerni omborga ID orqali biriktirish. Kuryer allaqachon boshqa
     * omborga biriktirilgan bo'lsa — ruxsat yo'q (avval u yerdan bo'shatiladi).
     */
    @Transactional
    public Courier assignCourierToWarehouse(UUID courierId, UUID warehouseId) {
        Courier courier = courierRepository.findById(courierId).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "COURIER_NOT_FOUND", "Courier not found"));
        if (courier.getWarehouseId() != null && !courier.getWarehouseId().equals(warehouseId)) {
            throw new DeliveryException(HttpStatus.BAD_REQUEST, "COURIER_BUSY",
                    "Bu kuryer boshqa omborga biriktirilgan — avval u yerdan bo'shating");
        }
        courier.setWarehouseId(warehouseId);
        Courier saved = courierRepository.save(courier);
        auditCourier(saved, "ATTACHED_WAREHOUSE", null, null, warehouseId, null);
        return saved;
    }

    /** Kuryerni ombordan bo'shatish (boshqa omborga o'tkazish uchun). */
    @Transactional
    public Courier unassignCourierFromWarehouse(UUID courierId) {
        Courier courier = courierRepository.findById(courierId).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "COURIER_NOT_FOUND", "Courier not found"));
        if (courier.getCurrentDeliveries() != null && courier.getCurrentDeliveries() > 0) {
            throw new DeliveryException(HttpStatus.BAD_REQUEST, "COURIER_HAS_DELIVERIES",
                    "Faol yetkazmasi bor kuryerni bo'shatib bo'lmaydi");
        }
        UUID prevWarehouse = courier.getWarehouseId();
        courier.setWarehouseId(null);
        Courier saved = courierRepository.save(courier);
        auditCourier(saved, "DETACHED_WAREHOUSE", null, null, prevWarehouse, null);
        return saved;
    }

    /**
     * Buyurtmani kuryerdan olib tashlash: yukini kamaytirib, delivery'ni
     * PENDING'ga qaytaradi (qayta tayinlash mumkin). Tarixga yoziladi.
     */
    @Transactional
    public DeliveryResponse unassignDelivery(UUID deliveryId, String reason) {
        Delivery d = getDelivery(deliveryId);
        if (d.getCourierId() == null) {
            throw new DeliveryException(HttpStatus.BAD_REQUEST, "NOT_ASSIGNED", "Delivery has no courier");
        }
        Courier courier = courierRepository.findById(d.getCourierId()).orElse(null);
        UUID courierId = d.getCourierId();
        String courierName = courier != null ? courier.getName() : null;
        d.setCourierId(null);
        d.setStatus(DeliveryStatus.PENDING);
        deliveryRepository.save(d);
        decreaseCourierLoad(courierId);
        logEvent(d.getId(), DeliveryStatus.PENDING, null,
                "Unassigned from courier" + (reason != null && !reason.isBlank() ? ": " + reason : ""));
        auditCourierRaw(courierId, courierName, "UNASSIGNED_DELIVERY",
                d.getId(), d.getOrderId(), null, reason);
        return toResponse(d);
    }

    /** Admin detail ko'rinishi: kuryerning barcha yetkazmalari + holatlari. */
    @Transactional(readOnly = true)
    public List<DeliveryResponse> courierDeliveriesById(UUID courierId) {
        getCourierById(courierId);
        return deliveryRepository.findByCourierIdOrderByCreatedAtDesc(courierId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<mexa.club.deliveryservice.entity.CourierActionAudit> courierAudit(UUID courierId, UUID actorId) {
        if (courierId != null) {
            return courierAuditRepository.findTop100ByCourierIdOrderByCreatedAtDesc(courierId);
        }
        if (actorId != null) {
            return courierAuditRepository.findTop100ByActorIdOrderByCreatedAtDesc(actorId);
        }
        return courierAuditRepository.findTop100ByOrderByCreatedAtDesc();
    }

    /** Kuryer ustidagi amalni tarixga yozadi: kim (admin/superadmin), nima, qachon. */
    private void auditCourier(Courier courier, String action,
                              UUID deliveryId, UUID orderId, UUID warehouseId, String reason) {
        auditCourierRaw(courier != null ? courier.getId() : null,
                courier != null ? courier.getName() : null,
                action, deliveryId, orderId, warehouseId, reason);
    }

    private void auditCourierRaw(UUID courierId, String courierName, String action,
                                 UUID deliveryId, UUID orderId, UUID warehouseId, String reason) {
        try {
            mexa.club.deliveryservice.entity.CourierActionAudit a =
                    new mexa.club.deliveryservice.entity.CourierActionAudit();
            a.setCourierId(courierId);
            a.setCourierName(courierName);
            a.setAction(action);
            a.setDeliveryId(deliveryId);
            a.setOrderId(orderId);
            a.setWarehouseId(warehouseId);
            a.setReason(reason);
            var auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof
                    mexa.club.deliveryservice.security.JwtUserPrincipal p) {
                a.setActorId(p.userId());
                a.setActorUsername(p.username());
            }
            if (auth != null) {
                boolean superAdmin = auth.getAuthorities().stream()
                        .anyMatch(g -> "ROLE_SUPER_ADMIN".equals(g.getAuthority()));
                boolean admin = auth.getAuthorities().stream()
                        .anyMatch(g -> "ROLE_ADMIN".equals(g.getAuthority()));
                a.setActorRole(superAdmin ? "SUPER_ADMIN" : admin ? "ADMIN" : "SYSTEM");
            } else {
                a.setActorRole("SYSTEM");
            }
            courierAuditRepository.save(a);
        } catch (Exception ex) {
            log.warn("Courier audit failed: {}", ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Courier getCourierById(UUID id) {
        return courierRepository.findById(id).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "COURIER_NOT_FOUND", "Courier not found"));
    }

    /** Rol almashtirishdan oldin tekshiruv: user kuryer sifatida qayerdadir biriktirilganmi. */
    @Transactional(readOnly = true)
    public Courier getCourierByUserId(UUID userId) {
        return courierRepository.findByUserId(userId).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "COURIER_NOT_FOUND", "Courier not found"));
    }

    @Transactional
    public void deleteCourier(UUID id) {
        Courier courier = courierRepository.findById(id).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "COURIER_NOT_FOUND", "Courier not found"));
        if (courier.getCurrentDeliveries() != null && courier.getCurrentDeliveries() > 0) {
            throw new DeliveryException(HttpStatus.BAD_REQUEST, "COURIER_HAS_DELIVERIES",
                    "Cannot delete courier with active deliveries");
        }
        auditCourier(courier, "DELETED", null, null, courier.getWarehouseId(), null);
        courierRepository.delete(courier);
    }

    @Transactional(readOnly = true)
    public List<Courier> searchCouriers(String search, Boolean active, String region) {
        return courierRepository.findByFilters(search, active, region);
    }

    @Transactional(readOnly = true)
    public CourierStatsResponse courierStats() {
        List<Courier> all = courierRepository.findAll();
        long total = all.size();
        long active = all.stream().filter(Courier::isActive).count();
        long totalCurrentDeliveries = all.stream()
                .mapToLong(c -> c.getCurrentDeliveries() != null ? c.getCurrentDeliveries() : 0)
                .sum();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1).atStartOfDay();
        LocalDateTime monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();

        long todayDeliveries = deliveryRepository.countDeliveriesSince(todayStart);
        long weeklyDeliveries = deliveryRepository.countDeliveriesSince(weekStart);
        long monthlyDeliveries = deliveryRepository.countDeliveriesSince(monthStart);

        long totalDelivered = deliveryRepository.countByStatus(DeliveryStatus.DELIVERED);
        long totalFailed = deliveryRepository.countByStatus(DeliveryStatus.FAILED);
        long totalCompleted = totalDelivered + totalFailed;
        double successRate = totalCompleted > 0 ? (double) totalDelivered / totalCompleted * 100.0 : 0.0;

        return new CourierStatsResponse(total, active, totalCurrentDeliveries,
                todayDeliveries, weeklyDeliveries, monthlyDeliveries, Math.round(successRate * 10.0) / 10.0);
    }

    /**
     * Omborga biriktirilgan har bir kuryer uchun yetkazmalar statistikasi:
     * jami, faol (kutilmoqda/biriktirilgan/olingan/yo'lda), yetkazilgan,
     * muvaffaqiyatsiz, qaytarilgan va bugungi yetkazmalar.
     */
    @Transactional(readOnly = true)
    public List<CourierWarehouseStatsResponse> courierStatsByWarehouse(UUID warehouseId) {
        List<Courier> couriers = courierRepository.findByWarehouseId(warehouseId);
        if (couriers.isEmpty()) {
            return List.of();
        }
        List<UUID> courierIds = couriers.stream().map(Courier::getId).toList();
        Map<UUID, List<Delivery>> byCourier = deliveryRepository.findByCourierIdIn(courierIds).stream()
                .collect(Collectors.groupingBy(Delivery::getCourierId));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return couriers.stream().map(c -> {
            List<Delivery> list = byCourier.getOrDefault(c.getId(), List.of());
            long total = list.size();
            long active = list.stream()
                    .map(Delivery::getStatus)
                    .filter(DeliveryService::isActiveDelivery)
                    .count();
            long delivered = list.stream()
                    .filter(d -> d.getStatus() == DeliveryStatus.DELIVERED)
                    .count();
            long failed = list.stream()
                    .filter(d -> d.getStatus() == DeliveryStatus.FAILED)
                    .count();
            long returned = list.stream()
                    .filter(d -> d.getStatus() == DeliveryStatus.RETURNED)
                    .count();
            long today = list.stream()
                    .filter(d -> d.getCreatedAt() != null && !d.getCreatedAt().isBefore(todayStart))
                    .count();
            return new CourierWarehouseStatsResponse(c.getId(), total, active, delivered, failed, returned, today);
        }).toList();
    }

    private static boolean isActiveDelivery(DeliveryStatus status) {
        return status == DeliveryStatus.PENDING
                || status == DeliveryStatus.ASSIGNED
                || status == DeliveryStatus.PICKED_UP
                || status == DeliveryStatus.IN_TRANSIT;
    }

    @Transactional(readOnly = true)
    public String exportCouriers() {
        List<Courier> couriers = courierRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Name,Phone,Region,Active,CurrentDeliveries\n");
        for (Courier c : couriers) {
            sb.append(c.getId()).append(",");
            sb.append(csvEscape(c.getName())).append(",");
            sb.append(csvEscape(c.getPhone())).append(",");
            sb.append(csvEscape(c.getRegion())).append(",");
            sb.append(c.isActive()).append(",");
            sb.append(c.getCurrentDeliveries() != null ? c.getCurrentDeliveries() : 0).append("\n");
        }
        return sb.toString();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Transactional(readOnly = true)
    public DeliveryFeeResponse calculateFee(String region, String district) {
        DeliveryFeeQuoteResponse q = quote(region, district, null, null, null);
        return new DeliveryFeeResponse(q.fee(), q.estimatedDays());
    }

    /**
     * Eski API uchun adapter — endi butun hisob yangi pricing engine'da
     * (formula + zona + surcharge + free delivery + cart incentive).
     * Formula: fee = base + distance*perKm + weight*perKg, min/max klamp,
     * keyin surcharges va free delivery/incentive chegirmalari qo'llanadi.
     */
    @Transactional(readOnly = true)
    public DeliveryFeeQuoteResponse quote(
            String region, String district,
            Double distanceKm, Double weightKg, BigDecimal subtotal) {
        var q = pricingService.calculate(new mexa.club.deliveryservice.pricing.PricingDtos.PricingRequest(
                null, region, district, distanceKm, weightKg, subtotal, null,
                java.time.LocalDateTime.now(), null, false));
        return new DeliveryFeeQuoteResponse(
                q.finalFee(),
                q.estimatedDays() != null ? q.estimatedDays() : 1,
                q.baseFee(),
                distanceKm != null ? distanceKm : 0.0,
                q.distanceFee(),
                weightKg != null ? weightKg : 0.0,
                q.weightFee(),
                q.freeDelivery(),
                null,
                null);
    }

    /** Ombor (lat1,lng1) va manzil (lat2,lng2) orasidagi masofa, km (Haversine). */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getById(UUID id) {
        return toResponse(getDelivery(id));
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> search(
            DeliveryStatus status, UUID courierId, LocalDateTime dateFrom, LocalDateTime dateTo,
            String region, String search, int page, int size
    ) {
        // Dinamik Specification: faqat berilgan filtrlar WHERE qo'shadi
        // (eski @Query null-param tipi aniqlanmasligi tufayli buzilgan edi).
        Specification<Delivery> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (courierId != null) {
                predicates.add(cb.equal(root.get("courierId"), courierId));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }
            if (region != null && !region.isBlank()) {
                predicates.add(cb.like(root.get("region"), "%" + region.trim() + "%"));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("recipientName"), like),
                        cb.like(root.get("recipientPhone"), like),
                        cb.like(root.get("deliveryAddress"), like),
                        cb.like(root.get("trackingCode"), like)
                ));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Delivery> result = deliveryRepository.findAll(spec, PageRequest.of(page, size));
        return result.map(this::toResponse).toList();
    }

    @Transactional
    public DeliveryResponse returnDelivery(UUID id, String reason) {
        Delivery d = getDelivery(id);
        if (d.getStatus() != DeliveryStatus.FAILED) {
            throw new DeliveryException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Only FAILED deliveries can be returned");
        }
        d.setStatus(DeliveryStatus.RETURNED);
        d.setFailReason(reason != null ? reason : "Returned");
        deliveryRepository.save(d);
        decreaseCourierLoad(d.getCourierId());
        logEvent(d.getId(), DeliveryStatus.RETURNED, null, "Delivery returned: " + (reason != null ? reason : ""));
        notifyOrderDeliveryFailed(d, "Delivery returned: " + (reason != null ? reason : ""));
        return toResponse(d);
    }

    @Transactional
    public DeliveryResponse updateNote(UUID id, DeliveryNoteRequest request) {
        Delivery d = getDelivery(id);
        d.setNote(request.note());
        return toResponse(deliveryRepository.save(d));
    }

    @Transactional
    public DeliveryResponse updateFee(UUID id, DeliveryFeeUpdateRequest request) {
        Delivery d = getDelivery(id);
        d.setDeliveryFee(request.fee());
        return toResponse(deliveryRepository.save(d));
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> allDeliveries() {
        return deliveryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryZone> allZones() {
        return zoneRepository.findAll();
    }

    @Transactional
    public DeliveryZone createZone(DeliveryZoneRequest request) {
        validateZoneRequest(request);
        zoneRepository.findByRegionIgnoreCaseAndDistrictIgnoreCase(
                request.region().trim(), request.district().trim()).ifPresent(z -> {
            throw new DeliveryException(HttpStatus.CONFLICT, "ZONE_DUPLICATE",
                    "Bu viloyat+tuman uchun zona allaqachon mavjud");
        });
        DeliveryZone zone = new DeliveryZone();
        applyZoneRequest(zone, request);
        DeliveryZone saved = zoneRepository.save(zone);
        auditZone(null, saved, "CREATE", "admin");
        return saved;
    }

    @Transactional
    public DeliveryZone updateZone(UUID id, DeliveryZoneRequest request) {
        validateZoneRequest(request);
        DeliveryZone zone = zoneRepository.findById(id).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND", "Zone not found"));
        zoneRepository.findByRegionIgnoreCaseAndDistrictIgnoreCase(
                request.region().trim(), request.district().trim())
                .filter(z -> !z.getId().equals(id))
                .ifPresent(z -> {
                    throw new DeliveryException(HttpStatus.CONFLICT, "ZONE_DUPLICATE",
                            "Bu viloyat+tuman uchun boshqa zona allaqachon mavjud");
                });
        DeliveryZone before = snapshotZone(zone);
        applyZoneRequest(zone, request);
        DeliveryZone saved = zoneRepository.save(zone);
        auditZone(before, saved, "UPDATE", "admin");
        return saved;
    }

    /** 3-bosqich: global standartni barcha zonalarga qo'llash. */
    @Transactional
    public int applyDefaultsToAll(DeliveryZoneRequest defaults) {
        validateZoneRequest(defaults);
        List<DeliveryZone> all = zoneRepository.findAll();
        for (DeliveryZone z : all) {
            DeliveryZone before = snapshotZone(z);
            z.setFee(defaults.fee());
            z.setPerKmFee(defaults.perKmFee() != null ? defaults.perKmFee() : BigDecimal.ZERO);
            z.setPerKgFee(defaults.perKgFee() != null ? defaults.perKgFee() : BigDecimal.ZERO);
            z.setFreeAbove(defaults.freeAbove());
            z.setEstimatedDays(defaults.estimatedDays());
            zoneRepository.save(z);
            auditZone(before, z, "BULK_APPLY", "admin");
        }
        return all.size();
    }

    @Transactional(readOnly = true)
    public List<mexa.club.deliveryservice.entity.DeliveryZoneAudit> zoneAudit(UUID zoneId) {
        if (zoneId != null) {
            return auditRepository.findTop50ByZoneIdOrderByCreatedAtDesc(zoneId);
        }
        return auditRepository.findTop50ByOrderByCreatedAtDesc();
    }

    private void validateZoneRequest(DeliveryZoneRequest request) {
        if (request.region() == null || request.region().isBlank()
                || request.district() == null || request.district().isBlank()) {
            throw new DeliveryException(HttpStatus.BAD_REQUEST, "VALIDATION",
                    "Viloyat va tuman majburiy");
        }
        if (request.fee() == null || request.fee().compareTo(BigDecimal.ZERO) < 0) {
            throw new DeliveryException(HttpStatus.BAD_REQUEST, "VALIDATION",
                    "Bazaviy narx manfiy bo'lishi mumkin emas");
        }
        if (request.estimatedDays() == null || request.estimatedDays() < 1 || request.estimatedDays() > 30) {
            throw new DeliveryException(HttpStatus.BAD_REQUEST, "VALIDATION",
                    "Kun 1..30 oralig'ida bo'lishi kerak");
        }
        if ((request.perKmFee() != null && request.perKmFee().compareTo(BigDecimal.ZERO) < 0)
                || (request.perKgFee() != null && request.perKgFee().compareTo(BigDecimal.ZERO) < 0)
                || (request.freeAbove() != null && request.freeAbove().compareTo(BigDecimal.ZERO) < 0)) {
            throw new DeliveryException(HttpStatus.BAD_REQUEST, "VALIDATION",
                    "km/kg/bepul chegara manfiy bo'lishi mumkin emas");
        }
    }

    private static DeliveryZone snapshotZone(DeliveryZone z) {
        DeliveryZone c = new DeliveryZone();
        c.setId(z.getId());
        c.setRegion(z.getRegion());
        c.setDistrict(z.getDistrict());
        c.setFee(z.getFee());
        c.setPerKmFee(z.getPerKmFee());
        c.setPerKgFee(z.getPerKgFee());
        c.setFreeAbove(z.getFreeAbove());
        c.setEstimatedDays(z.getEstimatedDays());
        return c;
    }

    private void auditZone(DeliveryZone before, DeliveryZone after, String action, String by) {
        try {
            mexa.club.deliveryservice.entity.DeliveryZoneAudit a =
                    new mexa.club.deliveryservice.entity.DeliveryZoneAudit();
            a.setZoneId(after.getId());
            a.setAction(action);
            a.setOldFee(before != null ? before.getFee() : null);
            a.setNewFee(after.getFee());
            a.setOldPerKmFee(before != null ? before.getPerKmFee() : null);
            a.setNewPerKmFee(after.getPerKmFee());
            a.setOldPerKgFee(before != null ? before.getPerKgFee() : null);
            a.setNewPerKgFee(after.getPerKgFee());
            a.setOldFreeAbove(before != null ? before.getFreeAbove() : null);
            a.setNewFreeAbove(after.getFreeAbove());
            a.setChangedBy(by);
            auditRepository.save(a);
        } catch (Exception ex) {
            log.warn("Zone audit failed: {}", ex.getMessage());
        }
    }

    private static void applyZoneRequest(DeliveryZone zone, DeliveryZoneRequest request) {
        zone.setRegion(request.region());
        zone.setDistrict(request.district());
        zone.setFee(request.fee());
        zone.setPerKmFee(request.perKmFee() != null ? request.perKmFee() : BigDecimal.ZERO);
        zone.setPerKgFee(request.perKgFee() != null ? request.perKgFee() : BigDecimal.ZERO);
        zone.setFreeAbove(request.freeAbove());
        zone.setMinFee(request.minFee());
        zone.setMaxFee(request.maxFee());
        if (request.priority() != null) zone.setPriority(request.priority());
        zone.setEstimatedDays(request.estimatedDays());
    }

    @Transactional
    public void deleteZone(UUID id) {
        DeliveryZone zone = zoneRepository.findById(id).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND", "Zone not found"));
        // 1-bosqich: faol delivery bor zonani o'chirishdan himoya
        long activeCount = deliveryRepository.findAll().stream()
                .filter(d -> d.getRegion() != null && d.getDistrict() != null
                        && d.getRegion().equalsIgnoreCase(zone.getRegion())
                        && d.getDistrict().equalsIgnoreCase(zone.getDistrict()))
                .filter(d -> d.getStatus() != DeliveryStatus.DELIVERED
                        && d.getStatus() != DeliveryStatus.RETURNED)
                .count();
        if (activeCount > 0) {
            throw new DeliveryException(HttpStatus.BAD_REQUEST, "ZONE_IN_USE",
                    "Bu zonada " + activeCount + " ta faol yetkazma bor — avval ularni yakunlang");
        }
        DeliveryZone before = snapshotZone(zone);
        zoneRepository.deleteById(id);
        auditZone(before, before, "DELETE", "admin");
    }

    private Delivery getCourierDelivery(UUID deliveryId, UUID courierUserId) {
        Courier courier = courierRepository.findByUserId(courierUserId).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "COURIER_NOT_FOUND", "Courier not found"));
        Delivery d = getDelivery(deliveryId);
        if (!courier.getId().equals(d.getCourierId())) {
            throw new DeliveryException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Delivery not assigned to courier");
        }
        return d;
    }

    private Delivery getDelivery(UUID id) {
        return deliveryRepository.findById(id).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Delivery not found"));
    }

    private void decreaseCourierLoad(UUID courierId) {
        if (courierId == null) {
            return;
        }
        courierRepository.findById(courierId).ifPresent(courier -> {
            int current = courier.getCurrentDeliveries() == null ? 0 : courier.getCurrentDeliveries();
            courier.setCurrentDeliveries(Math.max(0, current - 1));
            courierRepository.save(courier);
        });
    }

    private DeliveryResponse toResponse(Delivery d) {
        return mapper.toResponse(d, eventRepository.findByDeliveryIdOrderByCreatedAtAsc(d.getId()));
    }

    private void logEvent(UUID deliveryId, DeliveryStatus status, String location, String description) {
        DeliveryTrackingEvent event = new DeliveryTrackingEvent();
        event.setDeliveryId(deliveryId);
        event.setStatus(status.name());
        event.setLocation(location);
        event.setDescription(description);
        eventRepository.save(event);
    }

    private String nextTrackingCode() {
        String code;
        do {
            int rand = ThreadLocalRandom.current().nextInt(10000, 100000);
            code = "DLV-%d-%05d".formatted(Year.now().getValue(), rand);
        } while (deliveryRepository.findByTrackingCode(code).isPresent());
        return code;
    }
}
