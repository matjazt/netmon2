package com.matjazt.netmon2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for HTTP Basic Authentication.
 *
 * <p>This configuration:
 *
 * <ul>
 *   <li>Requires authentication for all endpoints
 *   <li>Uses HTTP Basic Auth (username/password in Authorization header)
 *   <li>Delegates user verification to CustomUserDetailsService
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * Configure security filter chain.
     *
     * <p>Spring Security 6.x uses lambda-based configuration.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Require authentication for all requests
                .authorizeHttpRequests(
                        auth ->
                                auth. // Allow unauthenticated access to health endpoint
                                        requestMatchers("/actuator/health", "/actuator/health/**")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                // Enable HTTP Basic Authentication
                .httpBasic(Customizer.withDefaults())
                // Disable CSRF for API (enable if you have a web UI with forms)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
