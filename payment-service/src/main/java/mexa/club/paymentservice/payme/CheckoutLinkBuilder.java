package mexa.club.paymentservice.payme;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import mexa.club.paymentservice.config.PaymeProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckoutLinkBuilder {

    private final PaymeProperties props;

    /** Checkout redirect URL: https://checkout.paycom.uz/&lt;base64&gt; */
    public String buildCheckoutUrl(Long orderId, Long amountTiyin, String returnUrl) {
        String raw = String.format(
                "m=%s;ac.order_id=%d;a=%d;c=%s", props.getMerchantId(), orderId, amountTiyin, returnUrl);
        String b64 = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return props.getCheckoutUrl().replaceAll("/+$", "") + "/" + b64;
    }
}
