package com.neong.vixie.repositories.user;

import com.neong.vixie.models.db.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    Optional<UserProfile> findByUserId(String userId);

    boolean existsByUsername(String username);

    Optional<UserProfile> findByUsername(String username);
}
