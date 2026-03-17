package com.neong.vixie.services.user;

import com.neong.vixie.helpers.api.IdGenerator;
import com.neong.vixie.models.constant.AuthProvider;
import com.neong.vixie.models.constant.Role;
import com.neong.vixie.models.db.User;
import com.neong.vixie.repositories.user.UserRepository;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User registerLocalUser(String email, String rawPassword, String username, String firstName, String lastName, String countryOfOrigin) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already in use");
        }
        if (username != null && !username.isBlank() && userRepository.existsByAppUsername(username)) {
            throw new IllegalArgumentException("Username is already in use");
        }
        String id = IdGenerator.generateId("users");
        User user = User.builder()
                .id(id)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .appUsername(username != null && !username.isBlank() ? username : email)
                .firstName(firstName)
                .lastName(lastName)
                .countryOfOrigin(countryOfOrigin)
                .role(Role.ROLE_USER)
                .authProvider(AuthProvider.LOCAL)
                .enabled(true)
                .locked(false)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User upsertOAuthUser(String email, String name, AuthProvider provider) {
        return userRepository.findByEmail(email)
                .map(existing -> {
                    existing.setAppUsername(name != null ? name : email);
                    existing.setAuthProvider(provider);
                    existing.setEnabled(true);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    String id = IdGenerator.generateId("users");
                    User user = User.builder()
                            .id(id)
                            .email(email)
                            .appUsername(name != null ? name : email)
                            .role(Role.ROLE_USER)
                            .authProvider(provider)
                            .enabled(true)
                            .locked(false)
                            .build();
                    return userRepository.save(user);
                });
    }

    @Transactional
    public void changePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
