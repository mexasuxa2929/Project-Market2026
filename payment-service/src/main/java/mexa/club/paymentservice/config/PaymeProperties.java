package mexa.club.paymentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "payme")
public class PaymeProperties {

    private String merchantId = "";
    private String testKey = "";
    private String prodKey = "";
    /** test | prod */
    private String activeKey = "test";
    private String checkoutUrl = "https://checkout.paycom.uz/";
    private String callbackPath = "/payment/payme/callback";
    private long timeoutMs = 12_000L;

    public String currentKey() {
        return "prod".equalsIgnoreCase(activeKey) ? prodKey : testKey;
    }
}
