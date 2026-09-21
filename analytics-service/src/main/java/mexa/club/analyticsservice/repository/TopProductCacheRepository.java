package mexa.club.analyticsservice.repository;

import mexa.club.analyticsservice.entity.TopProductCacheRow;
import mexa.club.analyticsservice.entity.TopProductCacheKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopProductCacheRepository extends JpaRepository<TopProductCacheRow, TopProductCacheKey> {

    void deleteByPeriod(String period);

    List<TopProductCacheRow> findByPeriodOrderByRankPositionAsc(String period);
}
