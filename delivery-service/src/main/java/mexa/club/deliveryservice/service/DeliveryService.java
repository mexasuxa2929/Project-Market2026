package mexa.club.deliveryservice.service;

import mexa.club.deliveryservice.client.OrderInternalClient;
import mexa.club.deliveryservice.dto.AssignCourierRequest;
import mexa.club.deliveryservice.dto.CourierRequest;
import mexa.club.deliveryservice.dto.CourierStatsResponse;
import mexa.club.deliveryservice.dto.CreateDeliveryRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DeliveryService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeliveryService.class);

    private final DeliveryRepository deliveryRepository;
    private final DeliveryTrackingEventRepository eventRepository;
    private final CourierRepository courierRepository;
    private final DeliveryZoneRepository zoneRepository;
    private final DeliveryMapper mapper;
    private final DeliveryNotificationService deliveryNotificationService;
    private final OrderInternalClient orderInternalClient;

    public DeliveryService(
            DeliveryRepository deliveryRepository,
            DeliveryTrackingEventRepository eventRepository,
            CourierRepository courierRepository,
            DeliveryZoneRepository zoneRepository,
            DeliveryMapper mapper,
            DeliveryNotificationService deliveryNotificationService,
            OrderInternalClient orderInternalClient
    ) {
        this.deliveryRepository = deliveryRepository;
        this.eventRepository = eventRepository;
        this.courierRepository = courierRepository;
        this.zoneRepository = zoneRepository;
        this.mapper = mapper;
        this.deliveryNotificationService = deliveryNotificationService;
        this.orderInternalClient = orderInternalClient;
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

    @Transactional
    public DeliveryResponse create(CreateDeliveryRequest request) {
        // Idempotency: order uchun delivery mavjud bo'lsa yangi yaratilmaydi (qayta CONFIRMED hollari).
        Delivery existing = deliveryRepository.findByOrderId(request.orderId()).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }
        DeliveryFeeResponse fee = calculateFee(request.region(), request.district());
        Delivery d = new Delivery();
        d.setOrderId(request.orderId());
        d.setFromWarehouseId(request.warehouseId());
        d.setDeliveryAddress(request.address());
        d.setRecipientName(request.recipientName());
        d.setRecipientPhone(request.recipientPhone());
        d.setRegion(request.region());
        d.setDistrict(request.district());
        d.setDeliveryFee(fee.fee());
        d.setEstimatedDelivery(LocalDate.now().plusDays(fee.estimatedDays()));
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
        deliveryNotificationService.notifyFailed(d, reason);
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
        return courierRepository.save(courier);
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
        return courierRepository.save(courier);
    }

    @Transactional(readOnly = true)
    public Courier getCourierById(UUID id) {
        return courierRepository.findById(id).orElseThrow(() ->
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
        DeliveryZone zone = zoneRepository.findByRegionIgnoreCaseAndDistrictIgnoreCase(region, district).orElse(null);
        if (zone == null) {
            return new DeliveryFeeResponse(BigDecimal.valueOf(15000), 2);
        }
        return new DeliveryFeeResponse(zone.getFee(), zone.getEstimatedDays());
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
        Page<Delivery> result = deliveryRepository.findByFilters(
                status, courierId, dateFrom, dateTo, region, search, PageRequest.of(page, size));
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
        DeliveryZone zone = new DeliveryZone();
        zone.setRegion(request.region());
        zone.setDistrict(request.district());
        zone.setFee(request.fee());
        zone.setEstimatedDays(request.estimatedDays());
        return zoneRepository.save(zone);
    }

    @Transactional
    public DeliveryZone updateZone(UUID id, DeliveryZoneRequest request) {
        DeliveryZone zone = zoneRepository.findById(id).orElseThrow(() ->
                new DeliveryException(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND", "Zone not found"));
        zone.setRegion(request.region());
        zone.setDistrict(request.district());
        zone.setFee(request.fee());
        zone.setEstimatedDays(request.estimatedDays());
        return zoneRepository.save(zone);
    }

    @Transactional
    public void deleteZone(UUID id) {
        zoneRepository.deleteById(id);
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
