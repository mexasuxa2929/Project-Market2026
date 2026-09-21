package mexa.club.shopservice.service;

import mexa.club.shopservice.dto.order.OrderPagePayload;
import mexa.club.shopservice.dto.order.OrderResponsePayload;
import mexa.club.shopservice.dto.order.OrderServiceEnvelope;
import mexa.club.shopservice.dto.order.ShopCreateOrderRequest;
import mexa.club.shopservice.exception.OrderProxyException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Service
public class OrderProxyService {

    private static final ParameterizedTypeReference<OrderServiceEnvelope<OrderPagePayload<OrderResponsePayload>>> PAGE_ENVELOPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<OrderServiceEnvelope<OrderResponsePayload>> SINGLE_ENVELOPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient orderRestClient;

    public OrderProxyService(RestClient orderRestClient) {
        this.orderRestClient = orderRestClient;
    }

    public OrderPagePayload<OrderResponsePayload> listMyOrders(String authorizationHeader, int page, int size) {
        OrderServiceEnvelope<OrderPagePayload<OrderResponsePayload>> env = orderRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/orders")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .headers(headers -> setAuth(headers, authorizationHeader))
                .retrieve()
                .body(PAGE_ENVELOPE);
        return unwrap(env);
    }

    public OrderResponsePayload getOrder(String authorizationHeader, UUID id) {
        OrderServiceEnvelope<OrderResponsePayload> env = orderRestClient.get()
                .uri("/api/orders/{id}", id)
                .headers(headers -> setAuth(headers, authorizationHeader))
                .retrieve()
                .body(SINGLE_ENVELOPE);
        return unwrap(env);
    }

    public OrderResponsePayload createOrder(String authorizationHeader, ShopCreateOrderRequest request) {
        OrderServiceEnvelope<OrderResponsePayload> env = orderRestClient.post()
                .uri("/api/orders")
                .headers(headers -> setAuth(headers, authorizationHeader))
                .body(request)
                .retrieve()
                .body(SINGLE_ENVELOPE);
        return unwrap(env);
    }

    public OrderResponsePayload cancelOrder(String authorizationHeader, UUID id, String reason) {
        OrderServiceEnvelope<OrderResponsePayload> env = orderRestClient.post()
                .uri("/api/orders/{id}/cancel", id)
                .headers(headers -> setAuth(headers, authorizationHeader))
                .body(reason != null && !reason.isBlank() ? Map.of("reason", reason) : Map.of())
                .retrieve()
                .body(SINGLE_ENVELOPE);
        return unwrap(env);
    }

    private static void setAuth(HttpHeaders headers, String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader)) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader.trim());
        }
    }

    private static <T> T unwrap(OrderServiceEnvelope<T> envelope) {
        if (envelope == null) {
            throw new OrderProxyException("Empty response from order-service");
        }
        if (!envelope.success() || envelope.data() == null) {
            String msg = envelope.message() != null ? envelope.message() : "order-service rejected request";
            throw new OrderProxyException(msg);
        }
        return envelope.data();
    }
}
