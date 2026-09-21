package mexa.club.paymentservice.transaction;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymeTransactionRepository extends JpaRepository<PaymeTransaction, Long> {

    Optional<PaymeTransaction> findByPaycomTxId(String paycomTxId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PaymeTransaction t where t.paycomTxId = :id")
    Optional<PaymeTransaction> findByPaycomTxIdForUpdate(@Param("id") String id);

    @Query(
            "select case when count(t) > 0 then true else false end from PaymeTransaction t "
                    + "where t.orderId = :orderId and t.paycomTxId <> :excludePaycomId and t.state in :states")
    boolean existsBlockingTransaction(
            @Param("orderId") Long orderId,
            @Param("excludePaycomId") String excludePaycomId,
            @Param("states") Collection<Short> states);

    List<PaymeTransaction> findByCreateTimeBetween(Long fromInclusive, Long toInclusive);
}
