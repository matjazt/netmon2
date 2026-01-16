package com.matjazt.netmon2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for password encoding.
 * 
 * BCryptPasswordEncoder is the recommended encoder for production use.
 * It automatically salts passwords and uses adaptive hashing.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Create PasswordEncoder bean.
     * 
     * This is used by Spring Security to:
     * - Encode passwords when creating users
     * - Compare provided passwords with stored passwords during authentication
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
