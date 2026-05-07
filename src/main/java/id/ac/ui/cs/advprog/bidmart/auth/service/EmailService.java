package id.ac.ui.cs.advprog.bidmart.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async 
    public void sendVerificationEmail(String to, String token) {
        String verificationUrl = "http://localhost:3000/verify?token=" + token;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Verify your BidMart Account");
        message.setText("Thank you for registering! Please click the link below to verify your account:\n" 
                        + verificationUrl);
        
        mailSender.send(message);
    }
}