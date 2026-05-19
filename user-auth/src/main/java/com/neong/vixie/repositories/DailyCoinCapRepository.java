package com.neong.vixie.repositories;

import com.neong.vixie.models.db.DailyCoinCap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyCoinCapRepository extends JpaRepository<DailyCoinCap, String> {

    Optional<DailyCoinCap> findByUser_IdAndTargetDate(String userId, LocalDate targetDate);
}
