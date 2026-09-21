package mexa.club.orderservice.client;

import mexa.club.orderservice.client.discount.DiscountApplyPayload;
import mexa.club.orderservice.client.discount.DiscountEnvelope;
import mexa.club.orderservice.client.discount.InternalDiscountApplyRequest;
import mexa.club.orderservice.client.discount.InternalDiscountConfirmRequest;
import mexa.club.orderservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "discountClient", url = "${services.discount.url}", configuration = FeignConfig.class)
public interface DiscountClient {

    @PostMapping("/internal/discounts/apply")
    DiscountEnvelope<DiscountApplyPayload> apply(@RequestBody InternalDiscountApplyRequest body);

    @PostMapping("/internal/discounts/confirm")
    DiscountEnvelope<Void> confirm(@RequestBody InternalDiscountConfirmRequest body);

    @PostMapping("/internal/discounts/cancel/{orderId}")
    DiscountEnvelope<Void> cancel(@PathVariable("orderId") UUID orderId);
}
