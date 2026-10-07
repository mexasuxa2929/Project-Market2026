package mexa.club.paymentservice.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import mexa.club.paymentservice.config.PaymeProperties;
import mexa.club.paymentservice.exception.PaymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymeMerchantClient {

    private static final Logger log = LoggerFactory.getLogger(PaymeMerchantClient.class);

    private final RestTemplate restTemplate;
    private final PaymeProperties props;
    private final ObjectMapper objectMapper;

    public PaymeMerchantClient(PaymeProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(props.getTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(props.getTimeoutMs()));
        this.restTemplate = new RestTemplate(requestFactory);
    }

    /** Payme merchant API'dan pulni qaytaradi (CancelTransaction). Xato bo'lsa PaymentException. */
    public void cancelTransaction(String paycomTxId, int reason) {
        String merchantId = props.getMerchantId();
        String secretKey = props.currentKey();
        if (merchantId == null || merchantId.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new PaymentException(HttpStatus.BAD_GATEWAY, "PAYME_NOT_CONFIGURED",
                    "Payme merchant credentials are not configured");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", 1);
        body.put("method", "CancelTransaction");
        body.put("params", Map.of(
                "id", paycomTxId,
                "time", System.currentTimeMillis(),
                "reason", reason));
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth", merchantId + ":" + secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String raw;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    props.getApiUrl(), HttpMethod.POST, entity, String.class);
            raw = response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.warn("Payme CancelTransaction HTTP {} for tx {}: {}", ex.getStatusCode().value(),
                    paycomTxId, ex.getMessage());
            throw paymeFailure(ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            log.warn("Payme CancelTransaction unreachable for tx {}: {}", paycomTxId, ex.getMessage());
            throw new PaymentException(HttpStatus.BAD_GATEWAY, "PAYME_UNAVAILABLE",
                    "Payme is unavailable: " + ex.getMessage());
        }

        if (raw == null || raw.isBlank()) {
            throw new PaymentException(HttpStatus.BAD_GATEWAY, "PAYME_ERROR", "Empty response from Payme");
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(raw);
        } catch (JsonProcessingException ex) {
            throw new PaymentException(HttpStatus.BAD_GATEWAY, "PAYME_ERROR",
                    "Unexpected response from Payme: " + raw);
        }
        JsonNode error = node.get("error");
        if (error != null && !error.isNull()) {
            log.warn("Payme CancelTransaction rejected for tx {}: {}", paycomTxId, error);
            throw paymeFailure(raw);
        }
    }

    private PaymentException paymeFailure(String body) {
        if (body != null && !body.isBlank()) {
            try {
                JsonNode error = objectMapper.readTree(body).get("error");
                if (error != null && !error.isNull()) {
                    JsonNode message = error.get("message");
                    if (message != null && message.isTextual()) {
                        return new PaymentException(HttpStatus.BAD_GATEWAY, "PAYME_ERROR", message.asText());
                    }
                    if (message != null && message.isObject()) {
                        JsonNode en = message.get("en");
                        if (en != null && en.isTextual()) {
                            return new PaymentException(HttpStatus.BAD_GATEWAY, "PAYME_ERROR", en.asText());
                        }
                        return new PaymentException(HttpStatus.BAD_GATEWAY, "PAYME_ERROR", message.toString());
                    }
                }
            } catch (JsonProcessingException ignored) {
                // non-JSON body — pastdagi umumiy xabar ishlatiladi
            }
            return new PaymentException(HttpStatus.BAD_GATEWAY, "PAYME_ERROR",
                    "Payme rejected the refund: " + body);
        }
        return new PaymentException(HttpStatus.BAD_GATEWAY, "PAYME_ERROR", "Payme rejected the refund");
    }
}
