package mexa.club.orderservice.client;

import mexa.club.orderservice.client.payload.InternalApiEnvelope;
import mexa.club.orderservice.client.payload.PaymentRefundRequest;
import mexa.club.orderservice.client.payload.PaymentRefundResponse;
import mexa.club.orderservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "paymentClient", url = "${services.payment.url}", configuration = FeignConfig.class, contextId = "paymentClient")
public interface PaymentServiceClient {

    @PostMapping("/internal/payments/refund")
    InternalApiEnvelope<PaymentRefundResponse> refund(@RequestBody PaymentRefundRequest body);
}
