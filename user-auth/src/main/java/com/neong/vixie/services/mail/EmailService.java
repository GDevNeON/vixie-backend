package com.neong.vixie.services.mail;

public interface EmailService {
    void sendOtpEmail(String to, String otpCode, String purpose);
}
