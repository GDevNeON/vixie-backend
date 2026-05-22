-- V12_0: Create notification_tokens table for FCM token registration
CREATE TABLE notification_tokens (
    id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    fcm_token VARCHAR(512) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_token_user_device UNIQUE (user_id, device_id)
);

CREATE INDEX idx_notification_tokens_user_id ON notification_tokens(user_id);
