package mexa.club.paymentservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class OrderServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceClient.class);

    private final RestTemplate restTemplate;
    private final String orderServiceUrl;
    private final String internalSecret;

    public OrderServiceClient(
            RestTemplate restTemplate,
            @Value("${services.order.url:http://localhost:8085}") String orderServiceUrl,
            @Value("${app.internal-secret}") String internalSecret
    ) {
        this.restTemplate = restTemplate;
        this.orderServiceUrl = orderServiceUrl;
        this.internalSecret = internalSecret;
    }

    public void confirmPayment(UUID orderServiceId) {
        call(orderServiceId, "payment-confirmed");
    }

    public void failPayment(UUID orderServiceId) {
        call(orderServiceId, "payment-failed");
    }

    private void call(UUID orderServiceId, String action) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.postForEntity(
                    orderServiceUrl + "/internal/orders/" + orderServiceId + "/" + action,
                    entity,
                    Void.class
            );
        } catch (Exception ex) {
            log.warn("order-service {} failed for orderServiceId={}: {}", action, orderServiceId, ex.getMessage());
        }
    }
}
