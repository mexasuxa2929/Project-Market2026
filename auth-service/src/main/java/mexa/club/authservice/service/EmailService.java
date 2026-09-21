package mexa.club.authservice.service;

public interface EmailService {

    void sendEmailVerificationOtp(String toEmail, String otpCode);

    void sendPasswordResetCode(String toEmail, String resetCode);
}

