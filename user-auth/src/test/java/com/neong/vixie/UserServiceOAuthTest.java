package com.neong.vixie;

import com.neong.vixie.models.constant.AuthProvider;
import com.neong.vixie.models.db.User;
import com.neong.vixie.services.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceOAuthTest {

    @Autowired
    private UserService userService;

    @Test
    void testUpsertAndFind() {
        String email = "testoauth@example.com";
        String name = "Test OAuth";

        // Try to upsert
        User savedUser = userService.upsertOAuthUser(email, name, AuthProvider.GOOGLE);

        assertNotNull(savedUser.getId());
        assertEquals(email, savedUser.getEmail());

        // Try to find it back
        Optional<User> found = userService.findByEmail(email);
        assertTrue(found.isPresent(), "User should be found after upsert");
        assertEquals(savedUser.getId(), found.get().getId());
    }
}
