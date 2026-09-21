package mexa.club.authservice.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sends verification OTP via {@link JavaMailSender}. Configure {@code spring.mail.*} in
 * {@code application.properties} (Gmail: App Password + {@code spring.mail.username}).
 */
@Slf4j
@Service
public class JavaMailEmailService implements EmailService {

    private static final String VERIFY_SUBJECT = "Mexa Market — Emailni tasdiqlang";
    private static final String RESET_SUBJECT = "Mexa Market — Parolni tiklash";

    private final JavaMailSender mailSender;
    private final String from;
    private final String mailUsername;
    private final String mailPassword;

    public JavaMailEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@example.com}") String from,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
    }

    @Override
    @Async
    public void sendEmailVerificationOtp(String toEmail, String otpCode) {
        sendHtmlEmail(toEmail, VERIFY_SUBJECT, buildVerifyHtml(otpCode), buildVerifyBody(otpCode), "verification");
    }

    @Override
    @Async
    public void sendPasswordResetCode(String toEmail, String resetCode) {
        sendHtmlEmail(toEmail, RESET_SUBJECT, buildResetHtml(resetCode), buildResetBody(resetCode), "password-reset");
    }

    private void sendHtmlEmail(String toEmail, String subject, String html, String fallbackText, String purpose) {
        if (!smtpConfigured()) {
            log.warn(
                    "{} email not sent (SMTP not configured): configure spring.mail.username and spring.mail.password "
                            + "(env MAIL_USERNAME/MAIL_PASSWORD). recipient={} body={}",
                    purpose,
                    toEmail,
                    fallbackText
            );
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            // From: "Mexa Market <mexasuxa@gmail.com>" ko'rinishida — inbox'da ishonchli nom
            String fromAddress = from.contains("@") ? from : "mexasuxa@gmail.com";
            try {
                helper.setFrom(fromAddress, "Mexa Market");
            } catch (Exception e) {
                helper.setFrom(fromAddress);
            }
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(fallbackText, html);
            helper.setReplyTo(fromAddress);
            message.setHeader("X-Mailer", "Mexa Market Mailer");
            message.setHeader("List-Unsubscribe", "<mailto:" + fromAddress + ">");
            mailSender.send(message);
            log.info("{} email sent successfully to {}", purpose, toEmail);
        } catch (MailException ex) {
            log.error("Failed to send {} email to {}. DEV fallback body={}", purpose, toEmail, fallbackText, ex);
        } catch (Exception ex) {
            log.error("Unexpected error while sending {} email to {}. DEV fallback body={}", purpose, toEmail, fallbackText, ex);
        }
    }

    // Eski nom bilan qolgan chaqiriqlar uchun (agar ishlatilsa)
    private void sendEmail(String toEmail, String subject, String body, String purpose) {
        sendHtmlEmail(toEmail, subject, buildVerifyHtml(body), body, purpose);
    }

    private boolean smtpConfigured() {
        return StringUtils.hasText(mailUsername) && StringUtils.hasText(mailPassword);
    }

    private static String buildVerifyBody(String otpCode) {
        return "Mexa Market — Tasdiqlash kodingiz: " + otpCode
                + "\n\nBu kod 5 daqiqa amal qiladi. Agar siz so'ramagan bo'lsangiz, e'tibor bermang.";
    }

    private static String buildResetBody(String resetCode) {
        return "Mexa Market — Parolni tiklash kodingiz: " + resetCode
                + "\n\nBu kod 15 daqiqa amal qiladi. Agar siz so'ramagan bo'lsangiz, e'tibor bermang.";
    }

    private static String buildVerifyHtml(String otpCode) {
        return """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 560px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 16px; overflow: hidden;">
                  <div style="background: #4F46E5; padding: 20px 24px; text-align: center;">
                    <div style="display: inline-block; background: #ffffff; color: #4F46E5; width: 40px; height: 40px; line-height: 40px; border-radius: 10px; font-weight: 800; font-size: 18px;">M</div>
                    <div style="color: #ffffff; font-weight: 700; font-size: 18px; margin-top: 8px;">Mexa Market</div>
                    <div style="color: #e0e7ff; font-size: 12px; margin-top: 4px;">Xarid qilish tajribangizni yuksaltiring</div>
                  </div>
                  <div style="padding: 24px;">
                    <h2 style="margin: 0 0 8px; font-size: 18px; color: #111827;">Emailni tasdiqlang</h2>
                    <p style="margin: 0 0 16px; font-size: 14px; color: #6b7280; line-height: 1.5;">Hisobingizni faollashtirish uchun quyidagi kodni ilovada kiriting:</p>
                    <div style="text-align: center; margin: 20px 0;">
                      <div style="display: inline-block; background: #f3f4f6; border: 1px dashed #d1d5db; border-radius: 12px; padding: 14px 28px; font-size: 28px; font-weight: 800; letter-spacing: 6px; color: #111827;">%s</div>
                    </div>
                    <p style="margin: 0; font-size: 13px; color: #6b7280;">Kod <b>5 daqiqa</b> amal qiladi. Agar siz so'ramagan bo'lsangiz, bu xatni e'tiborsiz qoldiring.</p>
                  </div>
                  <div style="background: #f9fafb; padding: 14px 24px; text-align: center; font-size: 11px; color: #9ca3af; border-top: 1px solid #e5e7eb;">
                    Mexa Market &middot; Avtomatik xat, javob berish shart emas
                  </div>
                </div>
                """.formatted(otpCode);
    }

    private static String buildResetHtml(String resetCode) {
        return """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 560px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 16px; overflow: hidden;">
                  <div style="background: #4F46E5; padding: 20px 24px; text-align: center;">
                    <div style="display: inline-block; background: #ffffff; color: #4F46E5; width: 40px; height: 40px; line-height: 40px; border-radius: 10px; font-weight: 800; font-size: 18px;">M</div>
                    <div style="color: #ffffff; font-weight: 700; font-size: 18px; margin-top: 8px;">Mexa Market</div>
                  </div>
                  <div style="padding: 24px;">
                    <h2 style="margin: 0 0 8px; font-size: 18px; color: #111827;">Parolni tiklash</h2>
                    <p style="margin: 0 0 16px; font-size: 14px; color: #6b7280; line-height: 1.5;">Parolingizni tiklash uchun quyidagi kodni kiriting:</p>
                    <div style="text-align: center; margin: 20px 0;">
                      <div style="display: inline-block; background: #f3f4f6; border: 1px dashed #d1d5db; border-radius: 12px; padding: 14px 28px; font-size: 28px; font-weight: 800; letter-spacing: 6px; color: #111827;">%s</div>
                    </div>
                    <p style="margin: 0; font-size: 13px; color: #6b7280;">Kod <b>15 daqiqa</b> amal qiladi. Agar siz so'ramagan bo'lsangiz, e'tiborsiz qoldiring.</p>
                  </div>
                  <div style="background: #f9fafb; padding: 14px 24px; text-align: center; font-size: 11px; color: #9ca3af; border-top: 1px solid #e5e7eb;">
                    Mexa Market &middot; Avtomatik xat, javob berish shart emas
                  </div>
                </div>
                """.formatted(resetCode);
    }
}
