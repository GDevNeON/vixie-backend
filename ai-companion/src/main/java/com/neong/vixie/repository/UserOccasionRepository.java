package com.neong.vixie.repository;

import com.neong.vixie.model.OccasionType;
import com.neong.vixie.model.UserOccasion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOccasionRepository extends JpaRepository<UserOccasion, String> {

    List<UserOccasion> findByUserId(String userId);

    Optional<UserOccasion> findByIdAndUserId(String id, String userId);

    Optional<UserOccasion> findByUserIdAndTypeAndOccasionDate(
            String userId, OccasionType type, String occasionDate);

    List<UserOccasion> findByUserIdAndOccasionDateAndNotificationEnabledTrue(
            String userId, String occasionDate);
}
