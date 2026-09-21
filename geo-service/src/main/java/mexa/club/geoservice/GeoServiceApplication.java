package mexa.club.geoservice;

import mexa.club.geoservice.security.InternalSecretValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableFeignClients(basePackages = "mexa.club.geoservice.client")
public class GeoServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GeoServiceApplication.class, args);
    }

    @Bean
    ApplicationRunner internalSecretStartupValidator(@Value("${app.internal.secret}") String internalSecret) {
        return args -> InternalSecretValidator.validate(internalSecret);
    }
}
