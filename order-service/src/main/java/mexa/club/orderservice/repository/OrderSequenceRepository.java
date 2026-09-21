package mexa.club.orderservice.repository;

import jakarta.persistence.LockModeType;
import mexa.club.orderservice.entity.OrderSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderSequenceRepository extends JpaRepository<OrderSequence, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from OrderSequence s where s.year = :year")
    Optional<OrderSequence> findByYearForUpdate(@Param("year") Integer year);
}
