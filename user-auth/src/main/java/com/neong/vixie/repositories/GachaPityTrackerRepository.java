package com.neong.vixie.repositories;

import com.neong.vixie.models.db.GachaPityTracker;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GachaPityTrackerRepository extends JpaRepository<GachaPityTracker, String> {

    Optional<GachaPityTracker> findByUser_IdAndBannerId(String userId, String bannerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GachaPityTracker g WHERE g.user.id = :userId AND g.bannerId = :bannerId")
    Optional<GachaPityTracker> findByUser_IdAndBannerIdForUpdate(@Param("userId") String userId, @Param("bannerId") String bannerId);
}
