package com.neong.vixie.services.auth;

import com.neong.vixie.helpers.api.IdGenerator;
import com.neong.vixie.models.constant.OtpPurpose;
import com.neong.vixie.models.db.OtpCode;
import com.neong.vixie.repositories.auth.OtpCodeRepository;

import com.neong.vixie.services.mail.EmailService;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_TTL_SECONDS = 300;

    private final OtpCodeRepository otpCodeRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpCodeRepository otpCodeRepository, EmailService emailService) {
        this.otpCodeRepository = otpCodeRepository;
        this.emailService = emailService;
    }

    public void sendRegistrationOtp(String email) {
        String code = generateAndStoreCode(email, OtpPurpose.REGISTRATION);
        log.info("Generated registration OTP for {}: {}", email, code);
        emailService.sendOtpEmail(email, code, "REGISTRATION");
    }

    public void sendForgotPasswordOtp(String email) {
        String code = generateAndStoreCode(email, OtpPurpose.PASSWORD_RESET);
        log.info("Generated password reset OTP for {}: {}", email, code);
        emailService.sendOtpEmail(email, code, "PASSWORD_RESET");
    }

    public void verifyOtp(String email, String code) {
        OtpCode otp = otpCodeRepository
                .findTopByEmailAndCodeAndConsumedIsFalseOrderByExpiresAtDesc(email, code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP code"));

        Instant now = Instant.now();
        if (now.isAfter(otp.getExpiresAt())) {
            throw new IllegalArgumentException("Invalid or expired OTP code");
        }

        if (!otp.isConsumed()) {
            otp.setConsumed(true);
            otpCodeRepository.save(otp);
        }
    }

    private String generateAndStoreCode(String email, OtpPurpose purpose) {
        otpCodeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .ifPresent(latestOtp -> {
                    Instant now = Instant.now();
                    if (now.isBefore(latestOtp.getCreatedAt().plusSeconds(30))) {
                        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
                                "Please wait before requesting a new OTP.");
                    }
                });

        String id = IdGenerator.generateId("otp");
        String code = String.format("%06d", random.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(OTP_TTL_SECONDS, ChronoUnit.SECONDS);

        OtpCode entity = new OtpCode();
        entity.setId(id);
        entity.setEmail(email);
        entity.setCode(code);
        entity.setPurpose(purpose);
        entity.setExpiresAt(expiresAt);
        entity.setConsumed(false);

        otpCodeRepository.save(entity);
        return code;
    }

    public boolean isEmailRecentlyVerified(String email) {
        return otpCodeRepository.findTopByEmailAndConsumedIsTrueOrderByUpdatedAtDesc(email)
                .map(otp -> {
                    Instant now = Instant.now();
                    // Check if the OTP was verified within the last 30 minutes
                    return otp.getUpdatedAt() != null &&
                           otp.getUpdatedAt().plus(30, ChronoUnit.MINUTES).isAfter(now);
                })
                .orElse(false);
    }
}
