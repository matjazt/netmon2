package com.matjazt.netmon2.config;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsProperties corsProperties;

    /**
     * Configure security filter chain.
     *
     * <p>Spring Security 6.x uses lambda-based configuration.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Apply CORS using the corsConfigurationSource bean below
                .cors(Customizer.withDefaults())
                // Require authentication for all requests
                .authorizeHttpRequests(
                        auth ->
                                auth. // Allow unauthenticated access to health endpoint and
                                        // internal error page
                                        requestMatchers(
                                                "/actuator/health", "/actuator/health/**", "/error")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                // Enable HTTP Basic Authentication
                .httpBasic(Customizer.withDefaults())
                // Disable CSRF for API (enable if you have a web UI with forms)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * CORS configuration source driven by {@link CorsProperties}.
     *
     * <p>Used automatically by Spring Security when {@code .cors(Customizer.withDefaults())} is
     * applied, because it searches the application context for a bean named {@code
     * corsConfigurationSource}.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // allowedOriginPatterns supports wildcards and works with allowCredentials=true,
        // unlike allowedOrigins("*") which is blocked by browsers when credentials are sent.
        config.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(corsProperties.getAllowedMethods());
        config.setAllowedHeaders(corsProperties.getAllowedHeaders());
        config.setAllowCredentials(corsProperties.isAllowCredentials());
        config.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
