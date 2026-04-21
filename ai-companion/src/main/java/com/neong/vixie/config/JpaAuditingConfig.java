package com.neong.vixie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing for @CreatedDate and @LastModifiedDate annotations.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
