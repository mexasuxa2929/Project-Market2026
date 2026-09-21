package mexa.club.notificationservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Eskiz (SMS) integratsiyasi — faqat {@code eskiz.enabled=true} bo'lganda ma'noli.
 * Token/email-parol bo'lmasa {@link mexa.club.notificationservice.service.SmsSenderService}
 * SMS yubormaydi va faqat log yozadi (email kanali ishlayveradi).
 */
@Configuration
@ConditionalOnProperty(prefix = "eskiz", name = "enabled", havingValue = "true")
public class EskizConfig {
}
