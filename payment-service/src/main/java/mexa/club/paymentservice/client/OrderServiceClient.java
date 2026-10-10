package mexa.club.paymentservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class OrderServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceClient.class);

    private final RestTemplate restTemplate;
    private final String orderServiceUrl;
    private final String internalSecret;
    private final PaymentOrderNotifyOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderServiceClient(
            RestTemplate restTemplate,
            @Value("${services.order.url:http://localhost:8085}") String orderServiceUrl,
            @Value("${app.internal-secret}") String internalSecret,
            PaymentOrderNotifyOutboxRepository outboxRepository
    ) {
        this.restTemplate = restTemplate;
        this.orderServiceUrl = orderServiceUrl;
        this.internalSecret = internalSecret;
        this.outboxRepository = outboxRepository;
    }

    public void confirmPayment(UUID orderServiceId, java.math.BigDecimal amountSom) {
        call(orderServiceId, "payment-confirmed", amountSom);
    }

    public void failPayment(UUID orderServiceId) {
        call(orderServiceId, "payment-failed", null);
    }

    private void call(UUID orderServiceId, String action, java.math.BigDecimal amountSom) {
        if (!send(orderServiceId, action, amountSom)) {
            // H4: xabar yetkazilmadi → outbox'ga yoziladi, retry job qayta yuboradi.
            enqueue(orderServiceId, action, amountSom);
        }
    }

    /** H4: xabarni yuboradi — muvaffaqiyat = true, xato = false. */
    public boolean send(UUID orderServiceId, String action, java.math.BigDecimal amountSom) {
        try {
            HttpHeaders headers = headers();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            StringBuilder url = new StringBuilder(orderServiceUrl)
                    .append("/internal/orders/").append(orderServiceId).append("/").append(action);
            if (amountSom != null) {
                url.append("?amount=").append(amountSom.toPlainString());
            }
            restTemplate.postForEntity(url.toString(), entity, Void.class);
            return true;
        } catch (Exception ex) {
            log.warn("order-service {} failed for orderServiceId={}: {}", action, orderServiceId, ex.getMessage());
            return false;
        }
    }

    void enqueue(UUID orderServiceId, String action, java.math.BigDecimal amountSom) {
        try {
            var existing = outboxRepository.findByOrderServiceIdAndActionAndSentAtIsNull(orderServiceId, action);
            if (existing.isPresent()) {
                // M-10: pending qatorda amount yangi bo'lsa yangilansin (retry ham tekshirsin).
                if (amountSom != null && existing.get().getAmountSom() == null) {
                    existing.get().setAmountSom(amountSom);
                    outboxRepository.save(existing.get());
                }
                return;
            }
            PaymentOrderNotifyOutbox row = new PaymentOrderNotifyOutbox();
            row.setId(UUID.randomUUID());
            row.setOrderServiceId(orderServiceId);
            row.setAction(action);
            row.setAmountSom(amountSom);
            row.setAttempts(0);
            row.setNextAttemptAt(LocalDateTime.now().plusSeconds(30));
            outboxRepository.save(row);
            log.info("Payment notify retry enqueued: orderServiceId={} action={}", orderServiceId, action);
        } catch (Exception ex) {
            log.warn("Payment outbox enqueue failed for {}: {}", orderServiceId, ex.getMessage());
        }
    }

    /** H4 reconcile: order-service'dan order'ni GET qilib o'qiydi (yoki null). */
    public JsonNode getOrder(UUID orderServiceId) {
        try {
            HttpHeaders headers = headers();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(
                    orderServiceUrl + "/internal/orders/" + orderServiceId,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    String.class
            );
            if (resp.getBody() == null) {
                return null;
            }
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode data = root.get("data");
            return data == null || data.isNull() ? null : data;
        } catch (Exception ex) {
            log.warn("order-service GET failed for orderServiceId={}: {}", orderServiceId, ex.getMessage());
            return null;
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
