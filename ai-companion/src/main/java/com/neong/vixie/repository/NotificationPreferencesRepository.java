package com.neong.vixie.repository;

import com.neong.vixie.model.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, String> {

    Optional<NotificationPreferences> findByUserIdAndCharacterId(String userId, String characterId);
}
