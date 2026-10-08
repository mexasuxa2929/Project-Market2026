package mexa.club.paymentservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mexa.club.paymentservice.order.Order;
import mexa.club.paymentservice.order.OrderRepository;
import mexa.club.paymentservice.order.OrderState;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * H4:
 * 1) Retry — order-service'ga yuborilmagan payment-confirmed/payment-failed
 *    outbox qatorlarini backoff bilan qayta yuboradi (1 min * 2^attempts, max 15 min).
 * 2) Reconcile (5 daqiqada bir) — PAID orderlar order-service'da ham PAID emasligini
 *    aniqlasa (crash oynasi uchun) CONFIRMED xabarini qayta navbatga qo'yadi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentNotifyRetryJob {

    static final String ACTION_CONFIRMED = "payment-confirmed";
    private static final int MAX_ATTEMPTS = 10;

    private final PaymentOrderNotifyOutboxRepository outboxRepository;
    private final OrderServiceClient orderServiceClient;
    private final OrderRepository paymentOrderRepository;

    @Scheduled(fixedDelayString = "${app.notify.retry-interval-ms:60000}")
    @Transactional
    public void retryDue() {
        for (PaymentOrderNotifyOutbox row : outboxRepository.findDue(LocalDateTime.now())) {
            if (orderServiceClient.send(row.getOrderServiceId(), row.getAction())) {
                row.setSentAt(LocalDateTime.now());
                outboxRepository.save(row);
                log.info("Payment outbox delivered: orderServiceId={} action={} attempts={}",
                        row.getOrderServiceId(), row.getAction(), row.getAttempts());
            } else {
                row.setAttempts(row.getAttempts() + 1);
                if (row.getAttempts() >= MAX_ATTEMPTS) {
                    log.error("Payment outbox gave up after {} attempts: orderServiceId={} action={}",
                            row.getAttempts(), row.getOrderServiceId(), row.getAction());
                    row.setNextAttemptAt(LocalDateTime.now().plusDays(1));
                } else {
                    long backoffSec = Math.min(60L * (1L << row.getAttempts()), 900L);
                    row.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSec));
                }
                outboxRepository.save(row);
            }
        }
    }

    /** H4 reconcile: PAID → order'da PAID emas → CONFIRMED qayta yuboriladi. */
    @Scheduled(fixedDelayString = "${app.reconcile.interval-ms:300000}")
    public void reconcilePaidOrders() {
        try {
            for (Order o : paymentOrderRepository
                    .findTop50ByStateAndOrderServiceIdNotNullOrderByCreatedAtDesc(OrderState.PAID)) {
                if (o.getOrderServiceId() == null) {
                    continue;
                }
                // 10 daqiqadan yangi orderlar retry job oralig'ida hal bo'ladi.
                if (o.getCreatedAt() != null && o.getCreatedAt().isAfter(Instant.now().minusSeconds(600))) {
                    continue;
                }
                JsonNode data = orderServiceClient.getOrder(o.getOrderServiceId());
                if (data == null) {
                    continue;
                }
                String paymentStatus = data.path("paymentStatus").asText("");
                if (!"PAID".equals(paymentStatus)) {
                    log.warn("Reconcile: order {} paymentStatus={} (payment PAID) → re-enqueue confirm",
                            o.getOrderServiceId(), paymentStatus);
                    orderServiceClient.enqueue(o.getOrderServiceId(), ACTION_CONFIRMED);
                }
            }
        } catch (Exception ex) {
            log.warn("Payment reconcile failed: {}", ex.getMessage());
        }
    }
}
