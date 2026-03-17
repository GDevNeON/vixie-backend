package com.neong.vixie.repositories.auth;

import com.neong.vixie.models.db.OtpCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpCodeRepository extends JpaRepository<OtpCode, String> {

    Optional<OtpCode> findTopByEmailAndCodeAndConsumedIsFalseOrderByExpiresAtDesc(String email, String code);

    Optional<OtpCode> findTopByEmailAndConsumedIsTrueOrderByUpdatedAtDesc(String email);

    Optional<OtpCode> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, com.neong.vixie.models.constant.OtpPurpose purpose);
}
