package com.neong.vixie.repository;

import com.neong.vixie.model.UserPreferences;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for user preferences (active companion selection).
 */
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, String> {

    Optional<UserPreferences> findByUserId(String userId);
}
