package mexa.club.orderservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import mexa.club.orderservice.OrderCreatedEvent;
import mexa.club.orderservice.client.AuthUserClient;
import mexa.club.orderservice.client.DeliveryClient;
import mexa.club.orderservice.client.GeoZoneClient;
import mexa.club.orderservice.client.DiscountClient;
import mexa.club.orderservice.client.PaymentServiceClient;
import mexa.club.orderservice.client.ProductServiceClient;
import mexa.club.orderservice.client.ShopInternalClient;
import mexa.club.orderservice.client.StockBatchClient;
import mexa.club.orderservice.client.WarehouseStockClient;
import mexa.club.orderservice.client.WarehousePurchaseClient;
import mexa.club.orderservice.client.discount.DiscountApplyPayload;
import mexa.club.orderservice.client.discount.DiscountEnvelope;
import mexa.club.orderservice.client.discount.InternalDiscountApplyRequest;
import mexa.club.orderservice.client.payload.DeliveryCreatedPayload;
import mexa.club.orderservice.client.payload.DeliveryQuotePayload;
import mexa.club.orderservice.client.payload.InternalAddressPayload;
import mexa.club.orderservice.client.payload.InternalApiEnvelope;
import mexa.club.orderservice.client.payload.InternalDeliveryCreateRequest;
import mexa.club.orderservice.client.payload.PaymentRefundRequest;
import mexa.club.orderservice.client.payload.PriceResolveResponse;
import mexa.club.orderservice.client.payload.ProductResponse;
import mexa.club.orderservice.client.payload.SalesOutRequestPayload;
import mexa.club.orderservice.client.payload.StockBatchByWarehouseResponse;
import mexa.club.orderservice.client.payload.StockBatchRequest;
import mexa.club.orderservice.client.payload.UserProfilePayload;
import mexa.club.orderservice.dto.AdminNoteRequest;
import mexa.club.orderservice.dto.CreateOrderItemRequest;
import mexa.club.orderservice.dto.CreateOrderRequest;
import mexa.club.orderservice.dto.InternalOrderStatsResponse;
import mexa.club.orderservice.dto.OrderPreviewResponse;
import mexa.club.orderservice.dto.OrderResponse;
import mexa.club.orderservice.dto.OrderStatsResponse;
import mexa.club.orderservice.dto.OrderStatusHistoryResponse;
import mexa.club.orderservice.dto.PagePayload;
import mexa.club.orderservice.dto.UpdateOrderStatusRequest;
import mexa.club.orderservice.entity.Order;
import mexa.club.orderservice.entity.OrderItem;
import mexa.club.orderservice.entity.OrderStatus;
import mexa.club.orderservice.entity.OrderStatusHistory;
import mexa.club.orderservice.entity.PaymentStatus;
import mexa.club.orderservice.exception.OrderServiceException;
import mexa.club.orderservice.realtime.OrderRealtimePublisher;
import mexa.club.orderservice.repository.OrderItemRepository;
import mexa.club.orderservice.repository.OrderRepository;
import mexa.club.orderservice.repository.OrderStatusHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final ProductServiceClient productServiceClient;
    private final WarehouseStockClient warehouseStockClient;
    private final StockBatchClient stockBatchClient;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OrderNumberService orderNumberService;
    private final OrderMapper orderMapper;
    /** Fallback only: used when no per-warehouse breakdown is available (e.g. legacy order rows). */
    private final UUID defaultWarehouseId;
    private final DiscountClient discountClient;
    private final OrderRealtimePublisher realtimePublisher;
    private final ShopInternalClient shopInternalClient;
    private final AuthUserClient authUserClient;
    private final DeliveryClient deliveryClient;
    private final WarehousePurchaseClient warehousePurchaseClient;
    private final GeoZoneClient geoZoneClient;
    private final GeocodingService geocodingService;
    private final PaymentServiceClient paymentServiceClient;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            ProductServiceClient productServiceClient,
            WarehouseStockClient warehouseStockClient,
            StockBatchClient stockBatchClient,
            ApplicationEventPublisher applicationEventPublisher,
            OrderNumberService orderNumberService,
            OrderMapper orderMapper,
            DiscountClient discountClient,
            OrderRealtimePublisher realtimePublisher,
            ShopInternalClient shopInternalClient,
            AuthUserClient authUserClient,
            DeliveryClient deliveryClient,
            WarehousePurchaseClient warehousePurchaseClient,
            GeoZoneClient geoZoneClient,
            GeocodingService geocodingService,
            PaymentServiceClient paymentServiceClient,
            ObjectMapper objectMapper,
            @Value("${app.warehouse.default-id}") UUID defaultWarehouseId
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.productServiceClient = productServiceClient;
        this.warehouseStockClient = warehouseStockClient;
        this.stockBatchClient = stockBatchClient;
        this.applicationEventPublisher = applicationEventPublisher;
        this.orderNumberService = orderNumberService;
        this.orderMapper = orderMapper;
        this.discountClient = discountClient;
        this.realtimePublisher = realtimePublisher;
        this.shopInternalClient = shopInternalClient;
        this.authUserClient = authUserClient;
        this.deliveryClient = deliveryClient;
        this.warehousePurchaseClient = warehousePurchaseClient;
        this.geoZoneClient = geoZoneClient;
        this.geocodingService = geocodingService;
        this.paymentServiceClient = paymentServiceClient;
        this.objectMapper = objectMapper;
        this.defaultWarehouseId = defaultWarehouseId;
    }

    @Transactional
    public OrderResponse create(UUID userId, CreateOrderRequest request) {
        if (userId == null) {
            throw new OrderServiceException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "JWT userId is missing");
        }
        Order order = new Order();
        order.setOrderNumber(orderNumberService.nextOrderNumber());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryAddressId(request.deliveryAddressId());
        if (request.deliveryAddress() != null && !request.deliveryAddress().isBlank()) {
            order.setDeliveryAddress(request.deliveryAddress());
        } else if (request.deliveryAddressId() != null) {
            order.setDeliveryAddress("Address snapshot: " + request.deliveryAddressId());
        }
        order.setNote(request.note());
        order.setCurrency("UZS");
        order.setPaymentMethod(normalizePaymentMethod(request.paymentMethod()));

        BigDecimal subtotal = BigDecimal.ZERO;
        Map<UUID, ProductResponse> productsById = new HashMap<>();
        for (CreateOrderItemRequest reqItem : request.items()) {
            // Variants are gone: each color is its own standalone Product, so the
            // productId the client sends already identifies the exact item/color —
            // no separate "resolve variant within product" step is needed anymore.
            ProductResponse product = fetchProduct(reqItem.productId());
            productsById.put(reqItem.productId(), product);
            BigDecimal unitPrice = fetchResolvedPrice(reqItem.productId(), reqItem.quantity());
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(reqItem.productId());
            item.setProductName(product.name());
            item.setImageUrl(product.imageUrls() != null && !product.imageUrls().isEmpty() ? product.imageUrls().get(0) : null);
            item.setQuantity(reqItem.quantity());
            item.setUnitPrice(unitPrice);
            item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(reqItem.quantity())));
            subtotal = subtotal.add(item.getSubtotal());
            order.getItems().add(item);
        }
        order.setSubtotal(subtotal);
        // Yetkazish narxi order saqlanishdan OLDIN hisoblanadi: manzil → teskari
        // geokodlash → delivery quote. Xato bo'lsa order yaratilmaydi (503) —
        // "bepul order" muammosi yo'q. (Avval: fee=0 saqlanib, afterCommit'da
        // tuzatilardi; xato bo'lsa order doim bepul qolardi.)
        DeliveryFeeQuote feeQuote = resolveDeliveryFee(request.deliveryAddressId(), subtotal);
        order.setDeliveryFee(feeQuote.fee());
        // Geo-routing: yetkazish manzili qaysi poligon ichida bo'lsa, order
        // usha poligonga biriktirilgan omborga yunaltiriladi. Topilmasa —
        // zaxira bo'yicha eski allocate (eng ko'p zaxiraga ega ombor).
        UUID preferredWarehouseId = resolvePreferredWarehouse(
                request.deliveryAddressId(), request.deliveryAddress());
        allocateWarehouseStock(order.getItems(), preferredWarehouseId);
        order.setEstimatedDeliveryDays(estimateDeliveryDays(order.getItems(), productsById));

        BigDecimal discountAmount = BigDecimal.ZERO;
        String discountCode = request.discountCode();
        if (discountCode != null && !discountCode.isBlank()) {
            try {
                List<UUID> productIds = order.getItems().stream()
                        .map(OrderItem::getProductId).toList();
                UUID tempOrderId = UUID.randomUUID();
                DiscountEnvelope<DiscountApplyPayload> discountResult = discountClient.apply(
                        new InternalDiscountApplyRequest(
                                tempOrderId,
                                userId,
                                discountCode.trim(),
                                subtotal,
                                productIds,
                                List.of()
                        )
                );
                if (discountResult != null && discountResult.data() != null) {
                    discountAmount = discountResult.data().discountAmount() != null
                            ? discountResult.data().discountAmount()
                            : BigDecimal.ZERO;
                    order.setDiscountCode(discountCode.trim());
                    order.setDiscountAmount(discountAmount);
                }
            } catch (FeignException ex) {
                discountAmount = BigDecimal.ZERO;
            }
        }

        BigDecimal total = subtotal.add(order.getDeliveryFee()).subtract(discountAmount).max(BigDecimal.ZERO);
        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);

        // Backorder: zaxira yetmagan qismga avtomatik purchase (omborga kirim),
        // so'ng to'liq salesOut. Purchase ishlamasa — faqat bor qismi chiqadi,
        // order baribir yaratiladi (bloklanmaydi).
        if (replenishShortfalls(saved)) {
            sendSalesOut(saved, null);
        } else {
            sendSalesOut(saved, coveredQuantities(saved));
        }
        saveStatusHistory(saved.getId(), null, OrderStatus.PENDING, userId, "Order created");
        sendNotificationAsync(saved);
        realtimePublisher.orderCreated(saved.getId(), saved.getUserId(), saved.getOrderNumber(), saved.getStatus().name());
        return orderMapper.toResponse(saved);
    }

    public OrderPreviewResponse preview(UUID userId, CreateOrderRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        List<UUID> productIds = new ArrayList<>();
        for (CreateOrderItemRequest reqItem : request.items()) {
            fetchProduct(reqItem.productId());
            productIds.add(reqItem.productId());
            BigDecimal unitPrice = fetchResolvedPrice(reqItem.productId(), reqItem.quantity());
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(reqItem.quantity())));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        String discountMessage = null;
        String discountCode = request.discountCode();
        if (discountCode != null && !discountCode.isBlank()) {
            try {
                DiscountEnvelope<DiscountApplyPayload> discountResult = discountClient.apply(
                        new InternalDiscountApplyRequest(
                                UUID.randomUUID(), userId, discountCode.trim(),
                                subtotal, productIds, List.of()));
                if (discountResult != null && discountResult.data() != null) {
                    discountAmount = discountResult.data().discountAmount() != null
                            ? discountResult.data().discountAmount()
                            : BigDecimal.ZERO;
                    discountMessage = discountResult.message();
                }
            } catch (FeignException ex) {
                discountAmount = BigDecimal.ZERO;
            }
        }

        DeliveryFeeQuote feeQuote = resolveDeliveryFee(request.deliveryAddressId(), subtotal);
        BigDecimal deliveryFee = feeQuote.fee();
        boolean freeDelivery = feeQuote.freeDelivery();
        String incentiveMessage = feeQuote.incentiveMessage();

        BigDecimal total = subtotal.subtract(discountAmount).add(deliveryFee).max(BigDecimal.ZERO);
        return new OrderPreviewResponse(subtotal, discountAmount, deliveryFee, total,
                freeDelivery, discountMessage, incentiveMessage);
    }

    /**
     * Yetkazish narxini manzil orqali hisoblaydi (manzil → teskari geokodlash →
     * delivery quote). Manzil yo'q bo'lsa — 0 (yetkazishsiz buyurtma).
     * Hisoblab bo'lmasa — 503 DELIVERY_PRICING_UNAVAILABLE: order yaratilmaydi,
     * "bepul order" muammosi yuzaga kelmaydi.
     */
    private DeliveryFeeQuote resolveDeliveryFee(UUID addressId, BigDecimal subtotal) {
        if (addressId == null) {
            return new DeliveryFeeQuote(BigDecimal.ZERO, false, null);
        }
        try {
            String region = "";
            String district = "";
            InternalAddressPayload address = shopInternalClient.getAddress(addressId);
            if (address != null && address.latitude() != null && address.longitude() != null) {
                Optional<GeocodingService.RegionDistrict> rd = geocodingService.reverse(
                        BigDecimal.valueOf(address.latitude()), BigDecimal.valueOf(address.longitude()));
                if (rd.isPresent()) {
                    region = rd.get().region();
                    district = rd.get().district();
                }
            }
            InternalApiEnvelope<DeliveryQuotePayload> quote = deliveryClient.quote(region, district, subtotal);
            if (quote == null || quote.data() == null) {
                throw new IllegalStateException("delivery quote bo'sh javob berdi");
            }
            BigDecimal fee = quote.data().finalFee() != null ? quote.data().finalFee() : BigDecimal.ZERO;
            if (fee.signum() < 0) {
                fee = BigDecimal.ZERO;
            }
            String incentive = quote.data().incentive() != null ? quote.data().incentive().message() : null;
            return new DeliveryFeeQuote(fee, Boolean.TRUE.equals(quote.data().freeDelivery()), incentive);
        } catch (Exception ex) {
            log.error("Delivery fee unavailable for address {}: {}", addressId, ex.getMessage());
            throw new OrderServiceException(HttpStatus.SERVICE_UNAVAILABLE, "DELIVERY_PRICING_UNAVAILABLE",
                    "Yetkazish narxini hisoblab bo'lmadi. Iltimos, keyinroq urinib ko'ring.");
        }
    }

    private record DeliveryFeeQuote(BigDecimal fee, boolean freeDelivery, String incentiveMessage) {}

    private static Map<UUID, Integer> coveredQuantities(Order order) {
        Map<UUID, Integer> map = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            int covered = item.getCoveredQuantity() != null ? item.getCoveredQuantity() : item.getQuantity();
            map.merge(item.getProductId(), covered, Math::max);
        }
        return map;
    }

    /**
     * Yetmagan miqdorlarga ombor bo'yicha guruhlab avtomatik purchase yaratadi
     * (invoice "AUTO-{orderNumber}"). Purchase darhol kirim qiladi, shuning
     * uchun keyin to'liq salesOut o'tadi. Xato bo'lsa false — order baribir
     * davom etadi (qisman salesOut fallback).
     */
    private boolean replenishShortfalls(Order order) {
        Map<UUID, List<OrderItem>> shortByWarehouse = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            int covered = item.getCoveredQuantity() != null ? item.getCoveredQuantity() : item.getQuantity();
            int shortfall = item.getQuantity() - covered;
            if (shortfall > 0) {
                UUID wh = item.getWarehouseId() != null ? item.getWarehouseId() : defaultWarehouseId;
                shortByWarehouse.computeIfAbsent(wh, k -> new ArrayList<>()).add(item);
            }
        }
        if (shortByWarehouse.isEmpty()) {
            return true;
        }
        try {
            for (Map.Entry<UUID, List<OrderItem>> e : shortByWarehouse.entrySet()) {
                List<WarehousePurchaseClient.AutoPurchaseItem> lines = new ArrayList<>();
                for (OrderItem item : e.getValue()) {
                    int shortfall = item.getQuantity()
                            - (item.getCoveredQuantity() != null ? item.getCoveredQuantity() : item.getQuantity());
                    if (shortfall > 0) {
                        lines.add(new WarehousePurchaseClient.AutoPurchaseItem(
                                item.getProductId(), BigDecimal.valueOf(shortfall)));
                    }
                }
                if (lines.isEmpty()) {
                    continue;
                }
                WarehousePurchaseClient.AutoPurchaseResponse resp = warehousePurchaseClient.autoPurchase(
                        e.getKey(),
                        new WarehousePurchaseClient.AutoPurchaseRequest(order.getOrderNumber(), lines));
                log.info("Auto-purchase {} created for order {} ({} lines)",
                        resp.invoiceNumber(), order.getOrderNumber(), resp.itemCount());
            }
            return true;
        } catch (Exception ex) {
            log.warn("Auto-purchase failed for order {}, falling back to partial sales-out: {}",
                    order.getOrderNumber(), ex.getMessage());
            return false;
        }
    }

    @Transactional(readOnly = true)
    public PagePayload<OrderResponse> myOrders(UUID userId, int page, int size) {
        Page<Order> result = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return new PagePayload<>(
                result.getContent().stream().map(orderMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PagePayload<OrderResponse> allOrders(int page, int size) {
        Page<Order> result = orderRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PagePayload<>(
                result.getContent().stream().map(orderMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PagePayload<OrderResponse> searchOrders(
            OrderStatus status,
            PaymentStatus paymentStatus,
            UUID userId,
            String orderNumber,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            UUID warehouseId,
            String search,
            int page,
            int size
    ) {
        Page<Order> result = orderRepository.searchOrders(
                status, paymentStatus, userId, orderNumber, dateFrom, dateTo, warehouseId, search,
                PageRequest.of(page, size));
        return new PagePayload<>(
                result.getContent().stream().map(orderMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id, UUID userId, boolean admin) {
        Order order = getOrder(id);
        if (!admin && !order.getUserId().equals(userId)) {
            throw new OrderServiceException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Order does not belong to user");
        }
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(UUID id, UUID userId, String reason) {
        Order order = getOrder(id);
        if (!order.getUserId().equals(userId)) {
            throw new OrderServiceException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Order does not belong to user");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderServiceException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Only PENDING order can be cancelled");
        }
        OrderStatus prev = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setClosedAt(LocalDateTime.now());
        order.setCancelReason(reason == null || reason.isBlank() ? "Cancelled by user" : reason);
        applyCancellationSideEffects(order, order.getCancelReason());
        saveStatusHistory(order.getId(), prev, OrderStatus.CANCELLED, userId, order.getCancelReason());
        Order savedCancel = orderRepository.save(order);
        realtimePublisher.orderStatusChanged(savedCancel.getId(), savedCancel.getUserId(), savedCancel.getStatus().name());
        return orderMapper.toResponse(savedCancel);
    }

    /**
     * H10: ruxsat etilgan holatlar jadvali — ma'nosi yo'q kechishlar
     * (masalan DELIVERED → SHIPPED, CANCELLED → CONFIRMED) oldini oladi.
     */
    private static final Map<OrderStatus, java.util.Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, java.util.EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, java.util.EnumSet.of(OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, java.util.EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, java.util.EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
            OrderStatus.DELIVERED, java.util.EnumSet.of(OrderStatus.REFUNDED),
            OrderStatus.CANCELLED, java.util.EnumSet.noneOf(OrderStatus.class),
            OrderStatus.REFUNDED, java.util.EnumSet.noneOf(OrderStatus.class)
    );

    @Transactional
    public OrderResponse updateStatus(UUID id, UUID changedBy, UpdateOrderStatusRequest request) {
        Order order = getOrder(id);
        OrderStatus prev = order.getStatus();
        OrderStatus next = request.status();
        // Idempotent: bir xil holatni qayta yuborish xato emas.
        if (next == prev) {
            return orderMapper.toResponse(order);
        }
        java.util.Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.get(prev);
        if (allowed == null || !allowed.contains(next)) {
            throw new OrderServiceException(HttpStatus.BAD_REQUEST, "INVALID_TRANSITION",
                    "Order holatini " + prev + " → " + next + " o'zgartirib bo'lmaydi");
        }
        order.setStatus(next);
        if (next == OrderStatus.REFUNDED) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        // Yopilish holatlari: daromad yopilish sanasiga bog'lanadi
        if (isClosedStatus(next)) {
            if (order.getClosedAt() == null) {
                order.setClosedAt(LocalDateTime.now());
            }
        } else {
            order.setClosedAt(null);
        }
        saveStatusHistory(order.getId(), prev, next, changedBy, request.reason());
        Order saved = orderRepository.save(order);
        realtimePublisher.orderStatusChanged(saved.getId(), saved.getUserId(), saved.getStatus().name());
        if (next == OrderStatus.CONFIRMED && prev != OrderStatus.CONFIRMED) {
            scheduleDeliveryCreation(saved);
        }
        // H10: bekor qilinish yon ta'sirlari — zaxira qaytarish, chegirma va delivery.
        if (next == OrderStatus.CANCELLED) {
            applyCancellationSideEffects(saved, request.reason());
        }
        return orderMapper.toResponse(saved);
    }

    /**
     * Buyurtma bekor qilinganda bajariladigan yon ta'sirlar (H10):
     * 1) warehouse zaxirasi qaytariladi, 2) chegirma bekor qilinadi,
     * 3) delivery bekor qilinadi (delivery-service ichki endpoint).
     * Har biri mustaqil — biri ishlamasa qolganlari ham bajariladi.
     */
    private void applyCancellationSideEffects(Order order, String reason) {
        try {
            reverseSalesOut(order);
        } catch (Exception ex) {
            log.warn("Reverse sales-out failed for cancelled order {}: {}", order.getId(), ex.getMessage());
        }
        if (order.getDiscountCode() != null) {
            try {
                discountClient.cancel(order.getId());
            } catch (FeignException ex) {
                log.warn("Discount cancel failed for order {}: {}", order.getId(), ex.getMessage());
            }
        }
        try {
            deliveryClient.cancelByOrderId(order.getId());
        } catch (Exception ex) {
            log.warn("Delivery cancel failed for order {}: {}", order.getId(), ex.getMessage());
        }
    }

    private static boolean isClosedStatus(OrderStatus status) {
        return status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED || status == OrderStatus.REFUNDED;
    }

    /**
     * Order CONFIRMED bo'lganda delivery-service'da yetkazish yozuvini yaratadi.
     * Tranzaksiya commit bo'lgandan keyin chaqiriladi (yetim delivery bo'lmasligi uchun).
     * Har qanday xatoda faqat log — order oqimi hech qachon uzilmaydi.
     */
    private void scheduleDeliveryCreation(Order order) {
        UUID orderId = order.getId();
        UUID userId = order.getUserId();
        UUID addressId = order.getDeliveryAddressId();
        String addressText = order.getDeliveryAddress();
        UUID warehouseId = order.getItems().stream()
                .map(OrderItem::getWarehouseId)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(defaultWarehouseId);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            createDeliveryForOrder(orderId, userId, addressId, addressText, warehouseId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                createDeliveryForOrder(orderId, userId, addressId, addressText, warehouseId);
            }
        });
    }

    private void createDeliveryForOrder(UUID orderId, UUID userId, UUID addressId, String addressText, UUID warehouseId) {
        try {
            if (addressId == null) {
                log.warn("Delivery not created for order {}: no deliveryAddressId (create manually)", orderId);
                return;
            }
            InternalAddressPayload address;
            try {
                address = shopInternalClient.getAddress(addressId);
            } catch (Exception ex) {
                log.warn("Delivery not created for order {}: address {} unresolved: {}", orderId, addressId, ex.getMessage());
                return;
            }
            if (address == null) {
                log.warn("Delivery not created for order {}: address {} empty response", orderId, addressId);
                return;
            }
            String region = "";
            String district = "";
            String phone = address.phone() == null ? "" : address.phone().trim();
            // Nuqta model: region/district teskari geokodlashdan olinadi
            if (address.latitude() != null && address.longitude() != null) {
                Optional<GeocodingService.RegionDistrict> rd = geocodingService.reverse(
                        BigDecimal.valueOf(address.latitude()), BigDecimal.valueOf(address.longitude()));
                if (rd.isPresent()) {
                    region = rd.get().region();
                    district = rd.get().district();
                }
            }
            // Telefon ixtiyoriy (mobile dialogdan olib tashlangan): bo'lmasa bo'sh
            // yuboriladi, delivery baribir yaratiladi
            String recipientName = "Customer";
            try {
                UserProfilePayload profile = authUserClient.getProfile(userId).data();
                if (profile != null && profile.username() != null && !profile.username().isBlank()) {
                    recipientName = profile.username();
                }
            } catch (Exception ex) {
                log.warn("Recipient profile unresolved for order {}: {}", orderId, ex.getMessage());
            }
            String fullAddress = addressText != null && !addressText.isBlank() ? addressText
                    : Stream.of(address.name(), address.line2())
                            .filter(s -> s != null && !s.isBlank())
                            .collect(Collectors.joining(", "));

            // Avtomatik mexanizm: order subtotal'ni uzatamiz — delivery-service
            // admin kiritgan zona tariflari orqali fee'ni o'zi hisoblaydi
            // (fee = baza + km + kg; subtotal >= freeAbove bo'lsa bepul).
            BigDecimal orderSubtotal = orderRepository.findById(orderId)
                    .map(Order::getSubtotal).orElse(null);
            // D-H1: ombor ↔ manzil masofasi (Haversine) — narx engine'da perKm uchun.
            Double distanceKm = resolveDistanceKm(warehouseId, address);
            DeliveryCreatedPayload created = deliveryClient
                    .createDelivery(new InternalDeliveryCreateRequest(
                            orderId, warehouseId, fullAddress, recipientName, phone,
                            region, district, null, orderSubtotal, distanceKm))
                    .data();
            log.info("Delivery created for order {}: trackingCode={}", orderId, created != null ? created.trackingCode() : "n/a");
            // Hisoblangan fee'ni order totaliga yozamiz: total = subtotal + fee - discount.
            if (created != null && created.deliveryFee() != null) {
                applyDeliveryFee(orderId, created.deliveryFee());
            }
        } catch (Exception ex) {
            log.warn("Delivery creation failed for order {}: {}", orderId, ex.getMessage());
        }
    }

    /**
     * D-H1: ombor pozitsiyasi (geo-service) ↔ mijoz manzili (shop address lat/lng)
     * orasidagi Haversine masofasi, km. Qaysidir taraf topilmasa null — narx
     * engine masofasiz (0 km) hisoblaydi, delivery yaratish to'xtamaydi.
     */
    private Double resolveDistanceKm(UUID warehouseId, InternalAddressPayload address) {
        try {
            if (warehouseId == null || address == null
                    || address.latitude() == null || address.longitude() == null) {
                return null;
            }
            GeoZoneClient.GeoPositionEnvelope env = geoZoneClient.warehousePosition(warehouseId);
            if (env == null || !env.success() || env.data() == null
                    || env.data().latitude() == null || env.data().longitude() == null) {
                return null;
            }
            return haversineKm(env.data().latitude(), env.data().longitude(),
                    address.latitude(), address.longitude());
        } catch (Exception ex) {
            log.warn("Distance calc failed for warehouse {}: {}", warehouseId, ex.getMessage());
            return null;
        }
    }

    static Double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double km = r * c;
        // 0.1 km aniqligi yetarli (narx hisobi uchun).
        return Math.round(km * 10.0) / 10.0;
    }

    /**
     * Delivery-service hisoblagan fee'ni order'ga yozadi.
     * afterCommit callback ichidan chaqiriladi (alohida tranzaksiya).
     */
    @Transactional
    public void applyDeliveryFee(UUID orderId, BigDecimal fee) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        BigDecimal safeFee = fee != null && fee.compareTo(BigDecimal.ZERO) > 0 ? fee : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal subtotal = order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO;
        order.setDeliveryFee(safeFee);
        order.setTotalAmount(subtotal.add(safeFee).subtract(discount).max(BigDecimal.ZERO));
        orderRepository.save(order);
        log.info("Delivery fee applied for order {}: fee={}, total={}", orderId, safeFee, order.getTotalAmount());
    }

    /**
     * delivery-service DELIVERED deb belgilaganda chaqiriladi (internal).
     * Terminal holatlar (CANCELLED/REFUNDED) o'zgarmaydi — idempotent.
     */
    @Transactional
    public OrderResponse markDelivered(UUID id) {
        Order order = getOrder(id);
        OrderStatus prev = order.getStatus();
        if (prev == OrderStatus.DELIVERED || prev == OrderStatus.CANCELLED || prev == OrderStatus.REFUNDED) {
            return orderMapper.toResponse(order);
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setClosedAt(LocalDateTime.now());
        saveStatusHistory(order.getId(), prev, OrderStatus.DELIVERED, order.getUserId(), "Delivery completed");
        Order saved = orderRepository.save(order);
        realtimePublisher.orderStatusChanged(saved.getId(), saved.getUserId(), saved.getStatus().name());
        return orderMapper.toResponse(saved);
    }

    /**
     * delivery-service yetkazib berishni muvaffaqiyatsiz deb bildirganda chaqiriladi (internal).
     * Order CANCELLED bo'ladi, zaxira qaytariladi, chegirma bekor qilinadi.
     * Terminal holatlar (DELIVERED/CANCELLED/REFUNDED) o'zgarmaydi — idempotent.
     */
    @Transactional
    public OrderResponse deliveryFailed(UUID id, String reason) {
        Order order = getOrder(id);
        OrderStatus prev = order.getStatus();
        if (prev == OrderStatus.DELIVERED || prev == OrderStatus.CANCELLED || prev == OrderStatus.REFUNDED) {
            return orderMapper.toResponse(order);
        }
        order.setStatus(OrderStatus.CANCELLED);
        if (order.getClosedAt() == null) {
            order.setClosedAt(LocalDateTime.now());
        }
        order.setCancelReason(reason == null || reason.isBlank() ? "Delivery failed" : reason);
        try {
            reverseSalesOut(order);
        } catch (Exception ex) {
            // Zaxira qaytarish ishlamasa ham buyurtma bekor qilinadi — faqat log
            log.warn("Reverse sales-out failed for order {}, cancellation continues: {}", order.getId(), ex.getMessage());
        }
        if (order.getDiscountCode() != null) {
            try {
                discountClient.cancel(order.getId());
            } catch (FeignException ex) {
                // do not block cancellation
            }
        }
        saveStatusHistory(order.getId(), prev, OrderStatus.CANCELLED, order.getUserId(), order.getCancelReason());
        Order saved = orderRepository.save(order);
        realtimePublisher.orderStatusChanged(saved.getId(), saved.getUserId(), saved.getStatus().name());
        return orderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse markPayment(UUID id, boolean paid) {
        Order order = getOrder(id);
        order.setPaymentStatus(paid ? PaymentStatus.PAID : PaymentStatus.UNPAID);
        if (!paid && order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setClosedAt(LocalDateTime.now());
            order.setCancelReason("Payment failed");
            reverseSalesOut(order);
            saveStatusHistory(order.getId(), OrderStatus.PENDING, OrderStatus.CANCELLED, order.getUserId(), "Payment failed");
        }
        Order saved = orderRepository.save(order);
        if (saved.getStatus() == OrderStatus.CANCELLED) {
            realtimePublisher.orderStatusChanged(saved.getId(), saved.getUserId(), saved.getStatus().name());
        }
        return orderMapper.toResponse(saved);
    }

    /**
     * Admin refund: avval payment-service orqali haqiqiy pul qaytariladi (Payme),
     * so'ng order REFUNDED ga o'tkaziladi. Payment xato bersa — order o'zgarmaydi.
     */
    @Transactional
    public OrderResponse refund(UUID id, UUID changedBy, String reason) {
        Order order = getOrder(id);
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new OrderServiceException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_STATUS", "Only paid orders can be refunded");
        }
        if (order.getStatus() == OrderStatus.REFUNDED) {
            throw new OrderServiceException(HttpStatus.CONFLICT, "ALREADY_REFUNDED", "Order is already refunded");
        }
        try {
            paymentServiceClient.refund(new PaymentRefundRequest(id, reason));
        } catch (FeignException ex) {
            throw paymentRefundFailure(ex);
        }
        OrderStatus prev = order.getStatus();
        order.setStatus(OrderStatus.REFUNDED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setCancelReason(reason == null || reason.isBlank() ? "Refunded by admin" : reason);
        if (order.getClosedAt() == null) {
            order.setClosedAt(LocalDateTime.now());
        }
        try {
            reverseSalesOut(order);
        } catch (Exception ex) {
            // Zaxira qaytarish ishlamasa ham refund davom etadi — faqat log
            log.warn("Reverse sales-out failed for order {}, refund continues: {}", order.getId(), ex.getMessage());
        }
        if (order.getDiscountCode() != null) {
            try {
                discountClient.cancel(order.getId());
            } catch (FeignException ex) {
                // do not block refund
            }
        }
        saveStatusHistory(order.getId(), prev, OrderStatus.REFUNDED, changedBy, order.getCancelReason());
        Order saved = orderRepository.save(order);
        realtimePublisher.orderStatusChanged(saved.getId(), saved.getUserId(), saved.getStatus().name());
        return orderMapper.toResponse(saved);
    }

    /** payment-service refund xatosini order xatosiga aylantiradi (status/code/message saqlanadi). */
    private OrderServiceException paymentRefundFailure(FeignException ex) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        String code = "PAYMENT_SERVICE_ERROR";
        String message = "Payment service refund failed";
        int httpStatus = ex.status();
        if (httpStatus >= 400 && httpStatus < 600) {
            status = HttpStatus.valueOf(httpStatus);
        }
        try {
            JsonNode body = objectMapper.readTree(ex.contentUTF8());
            if (body != null && body.isObject()) {
                if (body.hasNonNull("code")) {
                    code = body.get("code").asText();
                }
                if (body.hasNonNull("message")) {
                    message = body.get("message").asText();
                }
            }
        } catch (Exception parseEx) {
            // Gateway/proxy HTML javobi — defolt xabar qoladi
        }
        return new OrderServiceException(status, code, message);
    }

    @Transactional
    public void deleteOrder(UUID id) {
        Order order = getOrder(id);
        if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.REFUNDED) {
            throw new OrderServiceException(HttpStatus.BAD_REQUEST, "INVALID_STATUS",
                    "Only CANCELLED or REFUNDED orders can be deleted");
        }
        orderRepository.delete(order);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderHistory(UUID orderId) {
        return statusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(h -> new OrderStatusHistoryResponse(
                        h.getId(), h.getOrderId(), h.getFromStatus(), h.getToStatus(),
                        h.getChangedBy(), h.getReason(), h.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> exportOrders(
            OrderStatus status, PaymentStatus paymentStatus,
            LocalDateTime dateFrom, LocalDateTime dateTo
    ) {
        Page<Order> result = orderRepository.searchOrders(
                status, paymentStatus, null, null, dateFrom, dateTo, null, null,
                PageRequest.of(0, Integer.MAX_VALUE));
        return result.getContent().stream().map(orderMapper::toResponse).toList();
    }

    @Transactional
    public OrderResponse updateAdminNote(UUID id, AdminNoteRequest request) {
        Order order = getOrder(id);
        order.setAdminNote(request.note());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderStatsResponse stats() {
        LocalDateTime todayFrom = LocalDate.now().atStartOfDay();
        LocalDateTime todayTo = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LocalDate now = LocalDate.now();
        LocalDateTime monthFrom = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthTo = LocalDateTime.of(now.withDayOfMonth(now.lengthOfMonth()), LocalTime.MAX);

        OrderStatsResponse.Totals today = new OrderStatsResponse.Totals(
                orderRepository.countBetween(todayFrom, todayTo),
                orderRepository.revenueBetween(todayFrom, todayTo)
        );
        OrderStatsResponse.Totals thisMonth = new OrderStatsResponse.Totals(
                orderRepository.countBetween(monthFrom, monthTo),
                orderRepository.revenueBetween(monthFrom, monthTo)
        );
        Map<String, Long> byStatus = new HashMap<>();
        for (Object[] row : orderRepository.countByStatus()) {
            byStatus.put(String.valueOf(row[0]), (Long) row[1]);
        }
        long pendingCount = orderRepository.countPending();
        return new OrderStatsResponse(today, thisMonth, byStatus, pendingCount);
    }

    @Transactional(readOnly = true)
    public InternalOrderStatsResponse internalStats(LocalDateTime from, LocalDateTime to) {
        long orderCount = orderRepository.countBetween(from, to);
        BigDecimal revenue = orderRepository.revenueBetween(from, to);
        BigDecimal discountGiven = orderRepository.discountGivenBetween(from, to);
        long paidCount = orderRepository.countPaidBetween(from, to);
        long cancelledCount = orderRepository.countCancelledBetween(from, to);

        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        for (Object[] row : orderRepository.countByStatusBetween(from, to)) {
            statusBreakdown.put(row[0].toString(), (Long) row[1]);
        }

        Pageable top10 = PageRequest.of(0, 10);
        List<InternalOrderStatsResponse.TopProductStat> topProducts = new ArrayList<>();
        for (Object[] row : orderRepository.topProductsBetween(from, to, top10)) {
            topProducts.add(new InternalOrderStatsResponse.TopProductStat(
                    (UUID) row[0],
                    (String) row[1],
                    ((Number) row[2]).longValue(),
                    (BigDecimal) row[3]
            ));
        }

        return new InternalOrderStatsResponse(
                orderCount,
                revenue != null ? revenue : BigDecimal.ZERO,
                paidCount,
                cancelledCount,
                discountGiven != null ? discountGiven : BigDecimal.ZERO,
                statusBreakdown,
                topProducts
        );
    }

    private ProductResponse fetchProduct(UUID productId) {
        try {
            ProductResponse product = productServiceClient.getProduct(productId).data();
            if (product == null) {
                throw new OrderServiceException(HttpStatus.BAD_REQUEST, "PRODUCT_NOT_FOUND", "Product not found: " + productId);
            }
            return product;
        } catch (FeignException ex) {
            throw new OrderServiceException(HttpStatus.BAD_GATEWAY, "PRODUCT_SERVICE_ERROR", "Product service is unavailable");
        }
    }

    /**
     * Miqdorga bog'liq narx: 2+ liniya bo'lsa mos liniya narxi (muddatli chegirma ishlamaydi),
     * aks holda asosiy narx ustidan muddatli chegirma qo'llanadi. Resolve product-service'da.
     */
    private BigDecimal fetchResolvedPrice(UUID productId, int qty) {
        try {
            PriceResolveResponse price = productServiceClient.resolvePrice(productId, Math.max(1, qty)).data();
            if (price == null || price.unitPrice() == null) {
                throw new OrderServiceException(HttpStatus.BAD_REQUEST, "PRICE_NOT_FOUND", "Price not found for product " + productId);
            }
            return price.unitPrice();
        } catch (FeignException ex) {
            throw new OrderServiceException(HttpStatus.BAD_GATEWAY, "PRODUCT_SERVICE_ERROR", "Price lookup failed");
        }
    }

    /**
     * Picks, per order line, a warehouse that actually has enough available stock of that
     * product — instead of always checking/decrementing a single hardcoded warehouse. Uses a
     * single batched call to warehouse-service for all distinct products in the order, then
     * greedily assigns the first warehouse with sufficient remaining stock (tracking running
     * consumption so two lines for the same product in one order don't double-book the same
     * warehouse stock).
     */
    /**
     * Geo-routing: yetkazib berish manzili qaysi geo-poligon ichida bo'lsa,
     * order usha poligonga biriktirilgan omborga yunaltiriladi.
     * Manzil topilmasa / zona omborsiz bo'lsa null — stock-bo'yicha fallback.
     * Hech qachon orderni bloklamaydi.
     */
    private UUID resolvePreferredWarehouse(UUID addressId, String addressText) {
        try {
            // 1) Saqlangan manzilda nuqta bor bo'lsa — to'g'ridan-to'g'ri polygon
            // (geokodlash shart emas, "balo-battar" yo'q)
            Optional<GeocodingService.LatLng> point = resolveDeliveryPoint(addressId, addressText);
            if (point.isEmpty()) {
                return null;
            }
            GeoZoneClient.GeoZoneEnvelope env = geoZoneClient.checkPoint(
                    point.get().lat().toString(), point.get().lng().toString());
            if (env == null || !env.success() || env.data() == null) {
                return null;
            }
            return env.data().stream()
                    .map(GeoZoneClient.GeoZonePayload::warehouseId)
                    .filter(wh -> wh != null)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Geo-routing unavailable, fallback to stock-based allocation: {}", e.getMessage());
            return null;
        }
    }

    /** Yetkazish nuqtasi: avval saqlangan lat/lng, bo'lmasa matnni geokodlash. */
    private Optional<GeocodingService.LatLng> resolveDeliveryPoint(UUID addressId, String addressText) {
        if (addressId != null) {
            try {
                var a = shopInternalClient.getAddress(addressId);
                if (a != null) {
                    if (a.latitude() != null && a.longitude() != null) {
                        return Optional.of(new GeocodingService.LatLng(
                                BigDecimal.valueOf(a.latitude()), BigDecimal.valueOf(a.longitude())));
                    }
                    String q = Stream.of(a.name(), a.line2(), "Uzbekistan")
                            .filter(s -> s != null && !s.isBlank())
                            .collect(Collectors.joining(", "));
                    if (!q.isBlank()) {
                        Optional<GeocodingService.LatLng> geocoded = geocodingService.geocode(q);
                        if (geocoded.isPresent()) {
                            return geocoded;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Address {} unresolved for geo-routing: {}", addressId, e.getMessage());
            }
        }
        String query = buildGeoQuery(addressId, addressText);
        if (query.isBlank()) {
            return Optional.empty();
        }
        return geocodingService.geocode(query);
    }

    /** User kiritgan manzil matni: saqlangan manzil bo'lsa to'liq, aks holda erkin matn. */
    private String buildGeoQuery(UUID addressId, String addressText) {
        if (addressId != null) {
            try {
                var a = shopInternalClient.getAddress(addressId);
                if (a != null) {
                    String q = Stream.of(a.name(), a.line2(), "Uzbekistan")
                            .filter(s -> s != null && !s.isBlank())
                            .collect(Collectors.joining(", "));
                    if (!q.isBlank()) {
                        return q;
                    }
                }
            } catch (Exception e) {
                log.warn("Address {} unresolved for geo-routing: {}", addressId, e.getMessage());
            }
        }
        if (addressText != null && !addressText.isBlank()) {
            return addressText.trim() + ", Uzbekistan";
        }
        return "";
    }

    private void allocateWarehouseStock(List<OrderItem> items, UUID preferredWarehouseId) {
        List<UUID> productIds = items.stream().map(OrderItem::getProductId).distinct().toList();
        List<StockBatchByWarehouseResponse> breakdown;
        try {
            breakdown = stockBatchClient.getStockByWarehouse(new StockBatchRequest(productIds));
        } catch (FeignException ex) {
            // Ombor servisi ishlamasa ham order BLOKLANMAYDI: mavjudlik noma'lum
            // deb hisoblanadi (hammasi 0), defolt ombor tanlanadi, ETA = maxDays.
            log.warn("Stock check unavailable, proceeding without warehouse data: {}", ex.getMessage());
            breakdown = List.of();
        }
        Map<UUID, List<StockBatchByWarehouseResponse.WarehouseBreakdown>> byProduct = breakdown.stream()
                .collect(Collectors.toMap(StockBatchByWarehouseResponse::productId, StockBatchByWarehouseResponse::warehouses));

        Map<String, BigDecimal> remainingByProductWarehouse = new HashMap<>();

        for (OrderItem item : items) {
            List<StockBatchByWarehouseResponse.WarehouseBreakdown> options = byProduct.getOrDefault(item.getProductId(), List.of());
            BigDecimal needed = BigDecimal.valueOf(item.getQuantity());
            if (preferredWarehouseId != null) {
                // Geo-routing: poligon biriktirilgan ombor — zaxiradan qat'iy nazar
                // shu omborga yunaltiriladi; yetmagan qismga avtomatik purchase boradi.
                BigDecimal avail = availableAt(options, remainingByProductWarehouse,
                        item.getProductId(), preferredWarehouseId);
                int covered = avail.min(needed).intValue();
                remainingByProductWarehouse.put(item.getProductId() + ":" + preferredWarehouseId,
                        avail.subtract(BigDecimal.valueOf(covered)));
                item.setWarehouseId(preferredWarehouseId);
                item.setCoveredQuantity(covered);
                continue;
            }
            // Fallback (zona topilmadi): eng ko'p zaxiraga ega ombor.
            // Zaxira yetmasa ham order BLOKLANMAYDI (backorder): yetmagan qismga
            // avtomatik purchase yaratiladi (pastga qarang). coveredQuantity —
            // omborda topilgan miqdor, qoloni zavoddan keladi (maxDays).
            UUID chosenWarehouseId = null;
            BigDecimal bestAvailable = BigDecimal.ZERO;
            for (StockBatchByWarehouseResponse.WarehouseBreakdown wh : options) {
                String key = item.getProductId() + ":" + wh.warehouseId();
                BigDecimal available = remainingByProductWarehouse.computeIfAbsent(
                        key, k -> wh.availableQuantity() != null ? wh.availableQuantity() : BigDecimal.ZERO);
                if (available.compareTo(bestAvailable) > 0) {
                    bestAvailable = available;
                    chosenWarehouseId = wh.warehouseId();
                }
                if (available.compareTo(needed) >= 0) {
                    chosenWarehouseId = wh.warehouseId();
                    bestAvailable = available;
                    remainingByProductWarehouse.put(key, available.subtract(needed));
                    break;
                }
            }
            if (chosenWarehouseId == null) {
                chosenWarehouseId = defaultWarehouseId;
            } else if (bestAvailable.compareTo(needed) < 0) {
                // Qisman qoplash: qolgan zaxirani shu line ga belgilash
                String key = item.getProductId() + ":" + chosenWarehouseId;
                remainingByProductWarehouse.put(key, BigDecimal.ZERO);
            }
            int covered = bestAvailable.min(needed).intValue();
            item.setWarehouseId(chosenWarehouseId);
            item.setCoveredQuantity(covered);
        }
    }

    private static BigDecimal availableAt(
            List<StockBatchByWarehouseResponse.WarehouseBreakdown> options,
            Map<String, BigDecimal> remaining,
            UUID productId,
            UUID warehouseId) {
        String key = productId + ":" + warehouseId;
        BigDecimal cached = remaining.get(key);
        if (cached != null) {
            return cached;
        }
        for (StockBatchByWarehouseResponse.WarehouseBreakdown wh : options) {
            if (warehouseId.equals(wh.warehouseId())) {
                return wh.availableQuantity() != null ? wh.availableQuantity() : BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Orderning umumiy yetkazish muddati (kun): har itemning samarali muddati
     * ichidan eng kattasi. Item uchun: biriktirilgan omborda topilgan miqdor
     * yetarli bo'lsa → min (omborda bor), yetmasa → max (zavoddan zakaz).
     * Maydonlar bo'sh bo'lsa defolt 2–5 ishlatiladi.
     */
    private static int estimateDeliveryDays(List<OrderItem> items,
                                            Map<UUID, ProductResponse> products) {
        int eta = 0;
        for (OrderItem item : items) {
            ProductResponse p = products.get(item.getProductId());
            int min = (p != null && p.deliveryDaysMin() != null) ? p.deliveryDaysMin() : 2;
            int max = (p != null && p.deliveryDaysMax() != null) ? p.deliveryDaysMax() : 5;
            int covered = item.getCoveredQuantity() != null ? item.getCoveredQuantity() : 0;
            int effective = covered >= item.getQuantity() ? min : max;
            eta = Math.max(eta, effective);
        }
        return eta;
    }

    /**
     * @param qtyOverride productId → chiqariladigan miqdor; null bo'lsa to'liq
     *                    line miqdori (odatiy yo'l). Qisman salesOut fallback uchun.
     */
    private void sendSalesOut(Order order, Map<UUID, Integer> qtyOverride) {
        Map<UUID, List<OrderItem>> byWarehouse = order.getItems().stream()
                .collect(Collectors.groupingBy(i -> i.getWarehouseId() != null ? i.getWarehouseId() : defaultWarehouseId));
        // If a later warehouse fails, we must compensate the warehouses already
        // decremented so stock is not left inconsistent when the local order
        // transaction rolls back.
        List<Map.Entry<UUID, List<OrderItem>>> processed = new ArrayList<>();
        try {
            for (Map.Entry<UUID, List<OrderItem>> entry : byWarehouse.entrySet()) {
                List<SalesOutRequestPayload.Item> lines = new ArrayList<>();
                for (OrderItem i : entry.getValue()) {
                    int qty = qtyOverride != null
                            ? qtyOverride.getOrDefault(i.getProductId(), i.getQuantity())
                            : i.getQuantity();
                    if (qty > 0) {
                        lines.add(new SalesOutRequestPayload.Item(i.getProductId(), qty));
                    }
                }
                if (lines.isEmpty()) {
                    continue;
                }
                warehouseStockClient.salesOut(entry.getKey(), new SalesOutRequestPayload(
                        order.getId(),
                        lines
                ));
                processed.add(entry);
            }
        } catch (FeignException ex) {
            for (Map.Entry<UUID, List<OrderItem>> entry : processed) {
                try {
                    warehouseStockClient.reverseSalesOut(entry.getKey(), new SalesOutRequestPayload(
                            order.getId(),
                            entry.getValue().stream()
                                    .map(i -> new SalesOutRequestPayload.Item(i.getProductId(), i.getQuantity()))
                                    .toList()
                    ));
                } catch (FeignException reverseEx) {
                    log.error("Compensation reverseSalesOut failed for order {} warehouse {}",
                            order.getId(), entry.getKey(), reverseEx);
                }
            }
            // Ombor yozuvi ishlamasa ham order BLOKLANMAYDI: xato loglanadi,
            // qoldiq keyin sinxronlanadi. Mijoz orderni oladi.
            log.error("Sales-out failed for order {}, order still created: {}",
                    order.getOrderNumber(), ex.getMessage());
        }
    }

    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "CARD", "ONLINE");

    /** To'lov usuli validatsiyasi: ruxsat etilmagan/bo'sh qiymat -> CASH */
    private static String normalizePaymentMethod(String value) {
        if (value == null || value.isBlank()) {
            return "CASH";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return PAYMENT_METHODS.contains(normalized) ? normalized : "CASH";
    }

    private void reverseSalesOut(Order order) {
        Map<UUID, List<OrderItem>> byWarehouse = order.getItems().stream()
                .collect(Collectors.groupingBy(i -> i.getWarehouseId() != null ? i.getWarehouseId() : defaultWarehouseId));
        for (Map.Entry<UUID, List<OrderItem>> entry : byWarehouse.entrySet()) {
            try {
                warehouseStockClient.reverseSalesOut(entry.getKey(), new SalesOutRequestPayload(
                        order.getId(),
                        entry.getValue().stream()
                                .map(i -> new SalesOutRequestPayload.Item(i.getProductId(), i.getQuantity()))
                                .toList()
                ));
            } catch (FeignException ex) {
                // Bir ombor xatosi boshqa omborlarga ta'sir qilmaydi — log va davom
                log.warn("Reverse sales-out failed for order {} warehouse {}: {}", order.getId(), entry.getKey(), ex.getMessage());
            }
        }
    }

    private void sendNotificationAsync(Order order) {
        applicationEventPublisher.publishEvent(new OrderCreatedEvent(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getTotalAmount()
        ));
    }

    private Order getOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() ->
                new OrderServiceException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));
    }

    private void saveStatusHistory(UUID orderId, OrderStatus from, OrderStatus to, UUID changedBy, String reason) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setFromStatus(from != null ? from.name() : null);
        history.setToStatus(to.name());
        history.setChangedBy(changedBy);
        history.setReason(reason);
        history.setCreatedAt(LocalDateTime.now());
        statusHistoryRepository.save(history);
    }

    /** Mahsulotga bog'liq orderlar soni (o'chirishdan oldin tekshirish uchun). */
    public long countOrdersByProduct(UUID productId) {
        if (productId == null) {
            return 0;
        }
        return orderItemRepository.countByProductId(productId);
    }
}
