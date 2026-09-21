package mexa.club.notificationservice.service;

import mexa.club.notificationservice.dto.BulkNotificationRequest;
import mexa.club.notificationservice.dto.SendNotificationRequest;
import mexa.club.notificationservice.dto.TemplateUpdateRequest;
import mexa.club.notificationservice.entity.Notification;
import mexa.club.notificationservice.entity.NotificationChannel;
import mexa.club.notificationservice.entity.NotificationStatus;
import mexa.club.notificationservice.entity.NotificationTemplate;
import mexa.club.notificationservice.exception.NotificationException;
import mexa.club.notificationservice.realtime.NotificationRealtimePublisher;
import mexa.club.notificationservice.repository.NotificationRepository;
import mexa.club.notificationservice.repository.NotificationTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final TemplateRenderService renderService;
    private final EmailSenderService emailSenderService;
    private final SmsSenderService smsSenderService;
    private final NotificationRealtimePublisher realtimePublisher;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationTemplateRepository templateRepository,
            TemplateRenderService renderService,
            EmailSenderService emailSenderService,
            SmsSenderService smsSenderService,
            NotificationRealtimePublisher realtimePublisher
    ) {
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
        this.renderService = renderService;
        this.emailSenderService = emailSenderService;
        this.smsSenderService = smsSenderService;
        this.realtimePublisher = realtimePublisher;
    }

    @Transactional
    public Notification enqueue(SendNotificationRequest request) {
        NotificationTemplate template = templateRepository.findByType(request.type())
                .filter(NotificationTemplate::isActive)
                .orElseThrow(() -> new NotificationException(HttpStatus.BAD_REQUEST, "TEMPLATE_NOT_FOUND", "Template not found"));
        Notification n = new Notification();
        n.setUserId(request.userId());
        n.setType(request.type());
        n.setChannel(request.channel());
        n.setRecipientEmail(request.recipientEmail());
        n.setRecipientPhone(request.recipientPhone());
        n.setSubject(renderService.renderSubject(template, request.variables()));
        n.setBody(renderService.renderBody(template, request.variables()));
        n.setStatus(NotificationStatus.PENDING);
        n.setNextRetryAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(n);
        realtimePublisher.notificationCreated(saved.getId(), saved.getUserId(), saved.getType() == null ? null : saved.getType().name());
        return saved;
    }

    @Transactional
    public List<Notification> enqueueBulk(BulkNotificationRequest request) {
        return request.notifications().stream().map(this::enqueue).toList();
    }

    @Transactional(readOnly = true)
    public Page<Notification> all(int page, int size) {
        return notificationRepository.findAll(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Notification> my(UUID userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional
    public boolean markRead(UUID id, UUID userId) {
        return notificationRepository.markRead(id, userId) > 0;
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllRead(userId);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public Notification get(UUID id) {
        return notificationRepository.findById(id).orElseThrow(() ->
                new NotificationException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Notification not found"));
    }

    @Transactional
    public Notification resend(UUID id) {
        Notification n = get(id);
        n.setStatus(NotificationStatus.PENDING);
        n.setErrorMessage(null);
        n.setNextRetryAt(LocalDateTime.now());
        return notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplate> templates() {
        return templateRepository.findAll();
    }

    @Transactional
    public NotificationTemplate updateTemplate(UUID id, TemplateUpdateRequest req) {
        NotificationTemplate t = templateRepository.findById(id).orElseThrow(() ->
                new NotificationException(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "Template not found"));
        t.setSubject(req.subject());
        t.setBodyTemplate(req.bodyTemplate());
        t.setActive(req.active());
        return templateRepository.save(t);
    }

    @Transactional
    public void processPending() {
        List<Notification> list = notificationRepository.findTop200ByStatusInAndNextRetryAtBeforeOrderByCreatedAtAsc(
                List.of(NotificationStatus.PENDING, NotificationStatus.FAILED), LocalDateTime.now());
        for (Notification n : list) {
            try {
                dispatch(n);
                n.setStatus(NotificationStatus.SENT);
                n.setSentAt(LocalDateTime.now());
                n.setErrorMessage(null);
            } catch (Exception ex) {
                int retries = n.getRetryCount() + 1;
                n.setRetryCount(retries);
                n.setStatus(retries >= 3 ? NotificationStatus.DEAD_LETTER : NotificationStatus.FAILED);
                n.setErrorMessage(ex.getMessage());
                long backoffSeconds = (long) Math.pow(2, Math.min(retries, 6)) * 30;
                n.setNextRetryAt(LocalDateTime.now().plusSeconds(backoffSeconds));
            }
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void moveOldFailedToDeadLetter() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<Notification> oldFailed = notificationRepository.findByStatusAndCreatedAtBefore(NotificationStatus.FAILED, cutoff);
        for (Notification n : oldFailed) {
            n.setStatus(NotificationStatus.DEAD_LETTER);
            notificationRepository.save(n);
        }
    }

    private void dispatch(Notification n) throws Exception {
        if (n.getChannel() == NotificationChannel.EMAIL) {
            emailSenderService.send(n);
            return;
        }
        if (n.getChannel() == NotificationChannel.SMS) {
            smsSenderService.send(n);
            return;
        }
        // PUSH channel stub
    }
}
