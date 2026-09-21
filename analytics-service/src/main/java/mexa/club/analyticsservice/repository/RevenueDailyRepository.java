package mexa.club.analyticsservice.repository;

import mexa.club.analyticsservice.entity.RevenueDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RevenueDailyRepository extends JpaRepository<RevenueDaily, LocalDate> {

    List<RevenueDaily> findByRevenueDateBetweenOrderByRevenueDateAsc(LocalDate from, LocalDate to);
}
