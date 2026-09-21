package mexa.club.analyticsservice.client;

import mexa.club.analyticsservice.client.payload.AuthGrowthApiEnvelope;
import mexa.club.analyticsservice.client.payload.AuthStatsApiEnvelope;
import mexa.club.analyticsservice.config.InternalSecretFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;

@FeignClient(
        name = "authInternal",
        url = "${app.services.auth-url}",
        configuration = InternalSecretFeignConfig.class
)
public interface AuthInternalFeignClient {

    @GetMapping("/internal/auth/stats")
    AuthStatsApiEnvelope stats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to);

    @GetMapping("/internal/auth/users/growth")
    AuthGrowthApiEnvelope growth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);
}
