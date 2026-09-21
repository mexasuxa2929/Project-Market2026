package mexa.club.orderservice.client;

import mexa.club.orderservice.client.payload.InternalApiEnvelope;
import mexa.club.orderservice.client.payload.UserProfilePayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "authUserClient", url = "${services.auth.url}", configuration = mexa.club.orderservice.config.FeignConfig.class, contextId = "authUserClient")
public interface AuthUserClient {

    @GetMapping("/internal/auth/users/{id}/profile")
    InternalApiEnvelope<UserProfilePayload> getProfile(@PathVariable("id") UUID id);
}
