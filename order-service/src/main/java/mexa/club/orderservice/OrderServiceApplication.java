package mexa.club.orderservice;

import mexa.club.orderservice.security.InternalSecretValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    @Bean
    ApplicationRunner internalSecretStartupValidator(@Value("${app.internal-secret}") String internalSecret) {
        return args -> InternalSecretValidator.validate(internalSecret);
    }
}
