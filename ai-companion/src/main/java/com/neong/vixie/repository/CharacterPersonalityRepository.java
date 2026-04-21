package com.neong.vixie.repository;

import com.neong.vixie.model.CharacterPersonality;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for per-user character personality overrides.
 */
public interface CharacterPersonalityRepository extends JpaRepository<CharacterPersonality, String> {

    Optional<CharacterPersonality> findByUserIdAndCharacterId(String userId, String characterId);
}
