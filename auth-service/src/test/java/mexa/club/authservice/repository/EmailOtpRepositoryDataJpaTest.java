package mexa.club.authservice.repository;

import mexa.club.authservice.entity.EmailOtp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class EmailOtpRepositoryDataJpaTest {

    @Autowired
    private EmailOtpRepository emailOtpRepository;

    @Test
    void findTopByEmailOrderByCreatedAtDesc_returnsLatestOtp() {
        String email = "user@example.com";
        EmailOtp oldOtp = new EmailOtp(UUID.randomUUID(), email, "u1", "p1", "hash1", 0,
                LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5),
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        EmailOtp newOtp = new EmailOtp(UUID.randomUUID(), email, "u1", "p1", "hash2", 0,
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));

        emailOtpRepository.save(oldOtp);
        emailOtpRepository.save(newOtp);

        Optional<EmailOtp> found = emailOtpRepository.findTopByEmailOrderByCreatedAtDesc(email);
        assertTrue(found.isPresent());
        assertEquals("hash2", found.get().getCodeHash());
    }
}
