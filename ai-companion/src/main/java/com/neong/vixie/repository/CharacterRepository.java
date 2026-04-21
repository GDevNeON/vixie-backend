package com.neong.vixie.repository;

import com.neong.vixie.model.CharacterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for character definitions.
 */
public interface CharacterRepository extends JpaRepository<CharacterEntity, String> {
}
