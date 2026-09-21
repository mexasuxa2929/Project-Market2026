package mexa.club.paymentservice.payme;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import mexa.club.paymentservice.config.PaymeProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymeAuthFilter {

    private final PaymeProperties props;

    public boolean isValid(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
            return false;
        }
        try {
            String raw =
                    new String(Base64.getDecoder().decode(authorizationHeader.substring(6)), StandardCharsets.UTF_8);
            int colon = raw.indexOf(':');
            if (colon < 0) {
                return false;
            }
            String login = raw.substring(0, colon);
            String pass = raw.substring(colon + 1);
            if (!"Paycom".equals(login)) {
                return false;
            }
            String key = props.currentKey();
            if (key == null || key.isEmpty()) {
                return false;
            }
            byte[] a = pass.getBytes(StandardCharsets.UTF_8);
            byte[] b = key.getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(a, b);
        } catch (Exception e) {
            return false;
        }
    }
}
