package id.ac.ui.cs.advprog.bidmart.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(mailFrom);
        message.setSubject("Reset your BidMart password");
        message.setText("You requested a password reset. Click the link below to set a new password:\n"
                        + resetUrl
                        + "\n\nIf you did not request this, you can safely ignore this email.");

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", to);
        } catch (MailException exception) {
            log.error("Failed to send password reset email to {}", to, exception);
            throw exception;
        }
    }

    @Async 
    public void sendVerificationEmail(String to, String token) {
        String verificationUrl = frontendUrl + "/verify?token=" + token;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(mailFrom);
        message.setSubject("Verify your BidMart Account");
        message.setText("Thank you for registering! Please click the link below to verify your account:\n" 
                        + verificationUrl);

        try {
            mailSender.send(message);
            log.info("Verification email sent to {}", to);
        } catch (MailException exception) {
            log.error("Failed to send verification email to {}", to, exception);
            throw exception;
        }
    }
}