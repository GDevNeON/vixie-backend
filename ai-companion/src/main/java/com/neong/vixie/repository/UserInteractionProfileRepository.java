package com.neong.vixie.repository;

import com.neong.vixie.model.UserInteractionProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserInteractionProfile CRUD.
 * Phase 8: Used by UserInteractionProfileService to persist preference learning.
 */
@Repository
public interface UserInteractionProfileRepository extends JpaRepository<UserInteractionProfile, String> {

    Optional<UserInteractionProfile> findByUserId(String userId);
}
