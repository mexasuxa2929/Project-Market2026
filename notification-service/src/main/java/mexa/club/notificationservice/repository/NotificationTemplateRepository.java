package mexa.club.notificationservice.repository;

import mexa.club.notificationservice.entity.NotificationTemplate;
import mexa.club.notificationservice.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    Optional<NotificationTemplate> findByType(NotificationType type);
}
