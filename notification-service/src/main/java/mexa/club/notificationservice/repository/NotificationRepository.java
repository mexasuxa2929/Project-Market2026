package mexa.club.notificationservice.repository;

import mexa.club.notificationservice.entity.Notification;
import mexa.club.notificationservice.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<Notification> findTop200ByStatusInAndNextRetryAtBeforeOrderByCreatedAtAsc(List<NotificationStatus> statuses, LocalDateTime now);
    List<Notification> findByStatusAndCreatedAtBefore(NotificationStatus status, LocalDateTime before);

    long countByUserIdAndIsReadFalse(UUID userId);

    /** Admin ro'yxati: status + bepul matn bo'yicha filtr (T-H4 server pagination) */
    @Query("""
            SELECT n FROM Notification n
            WHERE (:status IS NULL OR n.status = :status)
              AND (:q IS NULL OR LOWER(COALESCE(n.recipientEmail, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                  OR LOWER(COALESCE(n.recipientPhone, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                  OR LOWER(COALESCE(n.subject, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
            """)
    Page<Notification> search(
            @Param("status") NotificationStatus status,
            @Param("q") String q,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.userId = :userId")
    int markRead(@Param("id") UUID id, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllRead(@Param("userId") UUID userId);
}
