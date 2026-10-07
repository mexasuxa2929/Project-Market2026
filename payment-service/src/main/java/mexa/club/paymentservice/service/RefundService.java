package mexa.club.paymentservice.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import mexa.club.paymentservice.client.PaymeMerchantClient;
import mexa.club.paymentservice.dto.RefundResponse;
import mexa.club.paymentservice.exception.PaymentException;
import mexa.club.paymentservice.order.Order;
import mexa.club.paymentservice.order.OrderRepository;
import mexa.club.paymentservice.order.OrderState;
import mexa.club.paymentservice.transaction.PaymeTransaction;
import mexa.club.paymentservice.transaction.PaymeTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    /** Payme bekor qilish sababi: 5 — mijozga pul qaytarildi (refund). */
    private static final int REFUND_REASON = 5;

    private final OrderRepository orderRepository;
    private final PaymeTransactionRepository txRepository;
    private final PaymeMerchantClient paymeMerchantClient;

    @Transactional
    public RefundResponse refund(UUID orderId, String reason) {
        if (orderId == null) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "orderId is required");
        }
        Order order = orderRepository.findByOrderServiceId(orderId).orElseThrow(() ->
                new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND",
                        "No payment found for order " + orderId));
        List<PaymeTransaction> transactions = txRepository.findByOrderId(order.getId());
        if (transactions.isEmpty()) {
            throw new PaymentException(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND",
                    "No payment transaction for order " + orderId);
        }

        PaymeTransaction captured = transactions.stream()
                .filter(t -> t.getState() != null && t.getState() == 2)
                .findFirst()
                .orElse(null);
        if (captured == null) {
            Optional<PaymeTransaction> refunded = transactions.stream()
                    .filter(t -> t.getState() != null && t.getState() == -2)
                    .findFirst();
            if (refunded.isPresent()) {
                return new RefundResponse(orderId, refunded.get().getPaycomTxId(), "REFUNDED");
            }
            throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_NOT_CAPTURED",
                    "No captured payment for order " + orderId);
        }

        log.info("Payme refund for order {} tx {}: {}", orderId, captured.getPaycomTxId(), reason);
        paymeMerchantClient.cancelTransaction(captured.getPaycomTxId(), REFUND_REASON);

        captured.setState((short) -2);
        captured.setReason(REFUND_REASON);
        captured.setCancelTime(System.currentTimeMillis());
        txRepository.save(captured);
        if (order.getState() != OrderState.CANCELLED) {
            order.setState(OrderState.CANCELLED);
            orderRepository.save(order);
        }
        return new RefundResponse(orderId, captured.getPaycomTxId(), "REFUNDED");
    }
}
