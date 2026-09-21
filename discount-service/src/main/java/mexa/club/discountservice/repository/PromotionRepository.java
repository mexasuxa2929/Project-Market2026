package mexa.club.discountservice.repository;

import jakarta.persistence.LockModeType;
import mexa.club.discountservice.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID>, JpaSpecificationExecutor<Promotion> {

    Optional<Promotion> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where lower(p.code) = lower(:code)")
    Optional<Promotion> findByCodeIgnoreCaseForUpdate(@Param("code") String code);

    @Query("select p from Promotion p where p.active = true and p.startsAt <= :now and (p.endsAt is null or p.endsAt >= :now) order by p.code asc")
    List<Promotion> findPublicActive(@Param("now") LocalDateTime now);

    @Query("select count(p) from Promotion p where p.active = true and p.startsAt <= :now and (p.endsAt is null or p.endsAt >= :now)")
    long countCurrentlyActive(@Param("now") LocalDateTime now);
}
