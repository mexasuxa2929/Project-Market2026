package mexa.club.paymentservice.payme.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymeRequestLogRepository extends JpaRepository<PaymeRequestLog, Long> {}
