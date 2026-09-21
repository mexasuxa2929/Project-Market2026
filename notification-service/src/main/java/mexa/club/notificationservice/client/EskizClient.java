package mexa.club.notificationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "eskizClient", url = "${eskiz.base-url}")
public interface EskizClient {
    @PostMapping(value = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> login(@RequestBody Map<String, String> body);

    @PostMapping(value = "/api/message/sms/send", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    Map<String, Object> send(@RequestHeader("Authorization") String token, @RequestBody String body);
}
