package com.neong.vixie.repository;

import com.neong.vixie.model.RelationshipState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for user-character relationship states (level + XP).
 */
public interface RelationshipStateRepository extends JpaRepository<RelationshipState, String> {

    Optional<RelationshipState> findByUserIdAndCharacterId(String userId, String characterId);
}
