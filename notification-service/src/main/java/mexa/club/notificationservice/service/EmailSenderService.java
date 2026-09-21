package mexa.club.notificationservice.service;

import jakarta.mail.internet.MimeMessage;
import mexa.club.notificationservice.entity.Notification;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {
    private final JavaMailSender mailSender;

    public EmailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(Notification n) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(n.getRecipientEmail());
        helper.setSubject(n.getSubject() == null ? "Notification" : n.getSubject());
        helper.setText(n.getBody(), true);
        mailSender.send(message);
    }
}
