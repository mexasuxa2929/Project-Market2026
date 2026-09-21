package mexa.club.analyticsservice.client;

import mexa.club.analyticsservice.client.payload.DiscountApiEnvelope;
import mexa.club.analyticsservice.config.InternalSecretFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@FeignClient(
        name = "discountInternal",
        url = "${app.services.discount-url}",
        configuration = InternalSecretFeignConfig.class
)
public interface DiscountInternalFeignClient {

    @GetMapping("/internal/discounts/stats")
    DiscountApiEnvelope stats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    );
}
