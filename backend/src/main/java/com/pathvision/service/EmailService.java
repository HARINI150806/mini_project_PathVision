package com.pathvision.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String to, String code) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setText("Your verification code is: " + code, true); // true = isHtml
            helper.setTo(to);
            helper.setSubject("Verify your email");
            helper.setFrom(fromEmail);
            
            mailSender.send(mimeMessage);
            System.out.println("Email sent to " + to + " with code: " + code);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send email");
        } catch (Exception e) {
            // Fallback for when email fails (e.g., no SMTP server configured)
            System.out.println("Failed to send email: " + e.getMessage());
            System.out.println("VERIFICATION CODE for " + to + ": " + code);
        }
    }
}
