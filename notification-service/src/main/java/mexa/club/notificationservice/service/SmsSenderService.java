package mexa.club.notificationservice.service;

import mexa.club.notificationservice.client.EskizClient;
import mexa.club.notificationservice.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class SmsSenderService {

    private static final Logger log = LoggerFactory.getLogger(SmsSenderService.class);

    private final EskizClient eskizClient;
    private final boolean smsEnabled;
    private final String email;
    private final String password;
    private final String sender;

    public SmsSenderService(
            EskizClient eskizClient,
            @Value("${eskiz.enabled:false}") boolean eskizEnabled,
            @Value("${eskiz.email:}") String email,
            @Value("${eskiz.password:}") String password,
            @Value("${eskiz.sender:4546}") String sender
    ) {
        this.eskizClient = eskizClient;
        this.email = email != null ? email.trim() : "";
        this.password = password != null ? password : "";
        this.sender = sender != null ? sender : "4546";
        this.smsEnabled = eskizEnabled && !this.email.isEmpty() && !this.password.isEmpty();
    }

    /**
     * Eskiz o'chirilgan yoki login ma'lumotlari yo'q bo'lsa — SMS yuborilmaydi (email kanali alohida).
     */
    public void send(Notification n) {
        if (!smsEnabled) {
            log.warn(
                    "SMS skipped (eskiz.enabled=false or ESKIZ_EMAIL/ESKIZ_PASSWORD missing). notificationId={}",
                    n.getId()
            );
            return;
        }
        Map<String, Object> login = eskizClient.login(Map.of("email", email, "password", password));
        Object data = login != null ? login.get("data") : null;
        if (!(data instanceof Map<?, ?> dataMap) || dataMap.get("token") == null) {
            log.error("Eskiz login response invalid for notificationId={}", n.getId());
            throw new IllegalStateException("Eskiz login failed");
        }
        String token = "Bearer " + String.valueOf(dataMap.get("token"));
        String body = "mobile_phone=" + UriUtils.encode(n.getRecipientPhone(), StandardCharsets.UTF_8)
                + "&message=" + UriUtils.encode(n.getBody(), StandardCharsets.UTF_8)
                + "&from=" + UriUtils.encode(sender, StandardCharsets.UTF_8);
        eskizClient.send(token, body);
    }
}
