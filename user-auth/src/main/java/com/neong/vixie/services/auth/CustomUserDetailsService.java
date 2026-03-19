package com.neong.vixie.services.auth;

import com.neong.vixie.models.db.User;
import com.neong.vixie.models.db.UserProfile;
import com.neong.vixie.repositories.user.UserProfileRepository;
import com.neong.vixie.repositories.user.UserRepository;
import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Try email first (most common login path)
        Optional<User> byEmail = userRepository.findByEmail(identifier);
        if (byEmail.isPresent()) {
            return byEmail.get();
        }

        // Fall back to username lookup via UserProfile
        Optional<UserProfile> byUsername = userProfileRepository.findByUsername(identifier);
        if (byUsername.isPresent()) {
            return byUsername.get().getUser();
        }

        throw new UsernameNotFoundException("User not found with identifier: " + identifier);
    }
}
