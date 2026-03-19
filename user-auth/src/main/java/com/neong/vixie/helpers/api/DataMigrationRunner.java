package com.neong.vixie.helpers.api;

import com.neong.vixie.models.db.User;
import com.neong.vixie.models.db.UserProfile;
import com.neong.vixie.repositories.user.UserProfileRepository;
import com.neong.vixie.repositories.user.UserRepository;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public DataMigrationRunner(
            JdbcTemplate jdbcTemplate,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrateUserProfiles();
    }

    private void migrateUserProfiles() {
        // Check if legacy columns exist before attempting migration
        boolean legacyColumnsExist;
        try {
            jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns "
                    + "WHERE table_name = 'users' AND column_name = 'username'",
                    Integer.class);
            legacyColumnsExist = true;
        } catch (Exception e) {
            log.info("Legacy columns not found in users table, skipping migration.");
            return;
        }

        // Find users that don't have a profile yet
        String sql = "SELECT user_id, username, first_name, last_name, country_of_origin "
                + "FROM users WHERE user_id NOT IN (SELECT user_id FROM user_profiles)";

        List<Map<String, Object>> legacyRows;
        try {
            legacyRows = jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.warn("Could not query legacy columns: {}. Migration skipped.", e.getMessage());
            return;
        }

        if (legacyRows.isEmpty()) {
            log.info("No users require profile migration.");
            return;
        }

        int migrated = 0;
        for (Map<String, Object> row : legacyRows) {
            String userId = (String) row.get("user_id");
            String legacyUsername = (String) row.get("username");
            String firstName = (String) row.get("first_name");
            String lastName = (String) row.get("last_name");
            String country = (String) row.get("country_of_origin");

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User {} not found during migration, skipping.", userId);
                continue;
            }

            // Build display name from firstName + lastName
            String displayName = ((firstName != null ? firstName : "")
                    + " " + (lastName != null ? lastName : "")).trim();
            if (displayName.isEmpty()) {
                displayName = user.getEmail().split("@")[0];
            }

            // Build username, fall back to email prefix
            String username = legacyUsername;
            if (username == null || username.isBlank()) {
                username = user.getEmail().split("@")[0];
            }

            // Ensure username uniqueness by appending suffix if needed
            String baseUsername = username;
            int suffix = 1;
            while (userProfileRepository.existsByUsername(username)) {
                username = baseUsername + "_" + suffix;
                suffix++;
            }

            UserProfile profile = UserProfile.builder()
                    .id(IdGenerator.generateId("user_profiles"))
                    .user(user)
                    .username(username)
                    .displayName(displayName)
                    .country(country)
                    .build();
            userProfileRepository.save(profile);
            migrated++;
        }

        log.info("Migrated {} user profile(s) from legacy data.", migrated);
    }
}
