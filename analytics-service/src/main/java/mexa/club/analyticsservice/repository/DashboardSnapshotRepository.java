package mexa.club.analyticsservice.repository;

import mexa.club.analyticsservice.entity.DashboardSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardSnapshotRepository extends JpaRepository<DashboardSnapshot, String> {}
