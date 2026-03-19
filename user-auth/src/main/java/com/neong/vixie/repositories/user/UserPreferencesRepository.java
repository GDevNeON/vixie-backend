package com.neong.vixie.repositories.user;

import com.neong.vixie.models.db.UserPreferences;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, String> {

    Optional<UserPreferences> findByUserId(String userId);
}
