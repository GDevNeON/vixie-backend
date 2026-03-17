package com.neong.vixie.repositories.user;

import com.neong.vixie.models.db.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailOrAppUsername(String email, String appUsername);

    boolean existsByEmail(String email);

    boolean existsByAppUsername(String appUsername);
}
