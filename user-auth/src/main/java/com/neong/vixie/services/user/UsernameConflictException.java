package com.neong.vixie.services.user;

public class UsernameConflictException extends RuntimeException {

    private final String username;

    public UsernameConflictException(String username) {
        super("Username '" + username + "' is already taken");
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
