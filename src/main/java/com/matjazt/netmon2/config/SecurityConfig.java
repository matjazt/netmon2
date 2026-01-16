package com.matjazt.netmon2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for HTTP Basic Authentication.
 * 
 * This configuration:
 * - Requires authentication for all endpoints
 * - Uses HTTP Basic Auth (username/password in Authorization header)
 * - Delegates user verification to CustomUserDetailsService
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configure security filter chain.
     * 
     * Spring Security 6.x uses lambda-based configuration.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Require authentication for all requests
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                // Enable HTTP Basic Authentication
                .httpBasic(Customizer.withDefaults())
                // Disable CSRF for API (enable if you have a web UI with forms)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
