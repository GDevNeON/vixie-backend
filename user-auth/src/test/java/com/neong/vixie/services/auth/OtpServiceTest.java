package com.neong.vixie.services.auth;

import com.neong.vixie.models.constant.OtpPurpose;
import com.neong.vixie.models.db.OtpCode;
import com.neong.vixie.repositories.auth.OtpCodeRepository;
import com.neong.vixie.services.mail.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
public class OtpServiceTest {

    @Mock
    private OtpCodeRepository otpCodeRepository;

    @Mock
    private EmailService emailService;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(otpCodeRepository, emailService);
    }

    @Test
    void sendRegistrationOtp_ShouldNotLogOtpCode(CapturedOutput output) {
        String email = "test@example.com";
        when(otpCodeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.REGISTRATION))
                .thenReturn(Optional.empty());

        otpService.sendRegistrationOtp(email);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(email), codeCaptor.capture(), eq("REGISTRATION"));
        String generatedCode = codeCaptor.getValue();

        // Ensure logs do not contain the code
        assertThat(output.getOut()).contains("Generated registration OTP for " + email);
        assertThat(output.getOut()).doesNotContain(generatedCode);
    }
}
