package com.neong.vixie.repository;

import com.neong.vixie.model.CreatorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreatorProfileRepository extends JpaRepository<CreatorProfile, String> {

    Optional<CreatorProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
