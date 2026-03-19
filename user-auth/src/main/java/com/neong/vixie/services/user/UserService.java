package com.neong.vixie.services.user;

import com.neong.vixie.helpers.api.IdGenerator;
import com.neong.vixie.models.constant.AuthProvider;
import com.neong.vixie.models.constant.Role;
import com.neong.vixie.models.db.User;
import com.neong.vixie.models.db.UserProfile;
import com.neong.vixie.repositories.user.UserProfileRepository;
import com.neong.vixie.repositories.user.UserRepository;

import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User registerLocalUser(String email, String rawPassword, String username,
                                  String firstName, String lastName, String countryOfOrigin) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already in use");
        }
        if (username != null && !username.isBlank()
                && userProfileRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already in use");
        }

        String userId = IdGenerator.generateId("users");
        User user = User.builder()
                .id(userId)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.ROLE_USER)
                .authProvider(AuthProvider.LOCAL)
                .enabled(true)
                .locked(false)
                .build();
        user = userRepository.save(user);

        String displayName = ((firstName != null ? firstName : "")
                + " " + (lastName != null ? lastName : "")).trim();
        if (displayName.isEmpty()) {
            displayName = email.split("@")[0];
        }

        UserProfile profile = UserProfile.builder()
                .id(IdGenerator.generateId("user_profiles"))
                .user(user)
                .username(username != null && !username.isBlank() ? username : email.split("@")[0])
                .displayName(displayName)
                .country(countryOfOrigin)
                .build();
        userProfileRepository.save(profile);

        return user;
    }

    @Transactional
    public User upsertOAuthUser(String email, String name, AuthProvider provider) {
        return userRepository.findByEmail(email)
                .map(existing -> {
                    existing.setAuthProvider(provider);
                    existing.setEnabled(true);
                    User saved = userRepository.save(existing);

                    userProfileRepository.findByUserId(existing.getId()).ifPresent(p -> {
                        if (name != null) {
                            p.setDisplayName(name);
                        }
                        userProfileRepository.save(p);
                    });

                    return saved;
                })
                .orElseGet(() -> {
                    String userId = IdGenerator.generateId("users");
                    User user = User.builder()
                            .id(userId)
                            .email(email)
                            .role(Role.ROLE_USER)
                            .authProvider(provider)
                            .enabled(true)
                            .locked(false)
                            .build();
                    user = userRepository.save(user);

                    UserProfile profile = UserProfile.builder()
                            .id(IdGenerator.generateId("user_profiles"))
                            .user(user)
                            .username(name != null ? name : email.split("@")[0])
                            .displayName(name != null ? name : email.split("@")[0])
                            .build();
                    userProfileRepository.save(profile);

                    return user;
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
