-- V1: Initial schema for vixie_user_auth
-- Mirrors JPA entities: User, UserProfile, UserPreferences, OtpCode

CREATE TABLE IF NOT EXISTS users (
    user_id          VARCHAR(64)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ,
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    email            VARCHAR(255) NOT NULL UNIQUE,
    password_hash    VARCHAR(255),
    auth_provider    VARCHAR(50)  NOT NULL DEFAULT 'LOCAL',
    role             VARCHAR(50)  NOT NULL DEFAULT 'USER',
    provider_user_id VARCHAR(255),
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    locked           BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT users_pkey PRIMARY KEY (user_id),
    CONSTRAINT users_auth_provider_check CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'FACEBOOK'))
);

CREATE TABLE IF NOT EXISTS user_profiles (
    profile_id    VARCHAR(64)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    user_id       VARCHAR(64)  NOT NULL UNIQUE,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    display_name  VARCHAR(100) NOT NULL,
    bio           VARCHAR(500),
    avatar_url    VARCHAR(2048),
    phone_number  VARCHAR(20),
    gender        VARCHAR(30),
    date_of_birth DATE,
    country       VARCHAR(10),
    location      VARCHAR(255),
    CONSTRAINT user_profiles_pkey PRIMARY KEY (profile_id),
    CONSTRAINT fk_user_profiles_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_preferences (
    preference_id      VARCHAR(64) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    user_id            VARCHAR(64) NOT NULL UNIQUE,
    locale             VARCHAR(10) DEFAULT 'en',
    timezone           VARCHAR(50),
    theme_mode         VARCHAR(10) DEFAULT 'system',
    notification_push  BOOLEAN DEFAULT TRUE,
    notification_email BOOLEAN DEFAULT TRUE,
    CONSTRAINT user_preferences_pkey PRIMARY KEY (preference_id),
    CONSTRAINT fk_user_preferences_user_id FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS otp_codes (
    otp_id     VARCHAR(64)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    email      VARCHAR(255) NOT NULL,
    code       VARCHAR(20)  NOT NULL,
    purpose    VARCHAR(50)  NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    consumed   BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT otp_codes_pkey PRIMARY KEY (otp_id),
    CONSTRAINT otp_codes_purpose_check CHECK (purpose IN ('REGISTRATION', 'PASSWORD_RESET'))
);
