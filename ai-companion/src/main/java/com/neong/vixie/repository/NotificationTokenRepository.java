package com.neong.vixie.repository;

import com.neong.vixie.model.NotificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTokenRepository extends JpaRepository<NotificationToken, String> {

    Optional<NotificationToken> findByUserIdAndDeviceId(String userId, String deviceId);

    List<NotificationToken> findByUserIdAndIsActiveTrue(String userId);
}
