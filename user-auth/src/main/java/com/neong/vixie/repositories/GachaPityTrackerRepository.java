package com.neong.vixie.repositories;

import com.neong.vixie.models.db.GachaPityTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GachaPityTrackerRepository extends JpaRepository<GachaPityTracker, String> {

    Optional<GachaPityTracker> findByUser_IdAndBannerId(String userId, String bannerId);
}
