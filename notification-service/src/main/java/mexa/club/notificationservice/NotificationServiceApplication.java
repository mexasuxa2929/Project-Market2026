package mexa.club.notificationservice;

import mexa.club.notificationservice.security.InternalSecretValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    @Bean
    ApplicationRunner internalSecretStartupValidator(@Value("${app.internal-secret}") String internalSecret) {
        return args -> InternalSecretValidator.validate(internalSecret);
    }
}
