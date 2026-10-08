package mexa.club.deliveryservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mexa.club.deliveryservice.client.OrderInternalClient;
import mexa.club.deliveryservice.client.payload.DeliveryFailedRequest;
import mexa.club.deliveryservice.entity.OrderNotifyOutbox;
import mexa.club.deliveryservice.repository.OrderNotifyOutboxRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * H2: order-service'ga yuborilmagan DELIVERED/FAILED xabarlarni qayta yuboradi.
 * Backoff: 1 min * 2^attempts (max 15 min), 10 urinishdan keyin 1 kunga kechiktiriladi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotifyRetryJob {

    static final String KIND_DELIVERED = "DELIVERED";
    static final String KIND_FAILED = "FAILED";
    private static final int MAX_ATTEMPTS = 10;

    private final OrderNotifyOutboxRepository outboxRepository;
    private final OrderInternalClient orderInternalClient;

    @Scheduled(fixedDelayString = "${app.notify.retry-interval-ms:60000}")
    @Transactional
    public void retryDue() {
        for (OrderNotifyOutbox row : outboxRepository.findDue(LocalDateTime.now())) {
            try {
                if (KIND_DELIVERED.equals(row.getKind())) {
                    orderInternalClient.markDelivered(row.getOrderId());
                } else {
                    orderInternalClient.deliveryFailed(row.getOrderId(),
                            new DeliveryFailedRequest(row.getReason()));
                }
                row.setSentAt(LocalDateTime.now());
                outboxRepository.save(row);
                log.info("Outbox notify delivered: order={} kind={} attempts={}",
                        row.getOrderId(), row.getKind(), row.getAttempts());
            } catch (Exception ex) {
                row.setAttempts(row.getAttempts() + 1);
                if (row.getAttempts() >= MAX_ATTEMPTS) {
                    log.error("Outbox notify gave up after {} attempts: order={} kind={} err={}",
                            row.getAttempts(), row.getOrderId(), row.getKind(), ex.getMessage());
                    row.setNextAttemptAt(LocalDateTime.now().plusDays(1));
                } else {
                    long backoffSec = Math.min(60L * (1L << row.getAttempts()), 900L);
                    row.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSec));
                }
                outboxRepository.save(row);
            }
        }
    }
}
