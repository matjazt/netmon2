package com.matjazt.netmon2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for password encoding.
 *
 * <p>BCryptPasswordEncoder is the recommended encoder for production use. It automatically salts
 * passwords and uses adaptive hashing.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Create PasswordEncoder bean.
     *
     * <p>This is used by Spring Security to:
     *
     * <ul>
     *   <li>Encode passwords when creating users
     *   <li>Compare provided passwords with stored passwords during authentication
     * </ul>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
