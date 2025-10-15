package com.example.LibraryManagement_Monolithic.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.frontend-url:http://localhost:8080/api/auth}")
    private String frontendUrl;

    public void sendVerificationEmail(String to, String token) {
        String subject = "Verify your Library account";
        String verifyLink = frontendUrl + "/verify?token=" + token;

        String content = """
                <h3>Welcome to Library Management System!</h3>
                <p>Click the link below to verify your email:</p>
                <a href="%s">%s</a>
                <br/><br/>
                <p>This link will expire in 24 hours.</p>
                """.formatted(verifyLink, verifyLink);

        sendHtmlMail(to, subject, content);
    }

    public void sendResetPasswordEmail(String to, String token) {
        String subject = "Reset your Library account password";
        String link = frontendUrl + "/reset-password?token=" + token;
        String content = """
                <h3>Password Reset Request</h3>
                <p>Click the link below to reset your password:</p>
                <a href="%s">%s</a>
                <p>If you did not request this, please ignore this email.</p>
                """.formatted(link, link);
        sendHtmlMail(to, subject, content);
    }

    public void sendChangeEmailVerification(String to, String token) {
        String subject = "Confirm your new email address";
        String link = frontendUrl + "/verify-change-email?token=" + token;
        String content = """
                <h3>Confirm New Email</h3>
                <p>Click to confirm your new email address:</p>
                <a href="%s">%s</a>
                """.formatted(link, link);
        sendHtmlMail(to, subject, content);
    }

    private void sendHtmlMail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML content

            mailSender.send(message);
//            log.info("📧 Sent email to {}", to);
        } catch (MessagingException e) {
//            log.error("❌ Failed to send email to {}", to, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}
