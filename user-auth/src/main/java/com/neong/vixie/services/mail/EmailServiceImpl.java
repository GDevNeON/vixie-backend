package com.neong.vixie.services.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Async
    @Override
    public void sendOtpEmail(String to, String otpCode, String purpose) {
        if (senderEmail == null || senderEmail.isEmpty() || "NOT_FOUND".equals(senderEmail)) {
            log.warn("SMTP Username is not configured. Email will NOT be sent to {}, Code: {}", to, otpCode);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(to);
            
            if ("REGISTRATION".equalsIgnoreCase(purpose)) {
                message.setSubject("Vixie - Email Verification Code");
                message.setText("Welcome to Vixie! Your email verification code is: " + otpCode + 
                                "\n\nThis code will expire in 5 minutes.\nPlease do not share this code with anyone.");
            } else if ("PASSWORD_RESET".equalsIgnoreCase(purpose)) {
                message.setSubject("Vixie - Password Reset Code");
                message.setText("We received a request to reset your password. Your password reset code is: " + otpCode + 
                                "\n\nThis code will expire in 5 minutes.\nIf you did not request this, please ignore this email.");
            } else {
                message.setSubject("Vixie - OTP Code");
                message.setText("Your OTP code is: " + otpCode);
            }

            javaMailSender.send(message);
            log.info("Successfully sent OTP email to {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage(), e);
        }
    }
}
