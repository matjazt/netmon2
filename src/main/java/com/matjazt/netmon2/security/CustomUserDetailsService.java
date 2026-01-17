package com.matjazt.netmon2.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.matjazt.netmon2.repository.AccountRepository;

/**
 * Custom UserDetailsService for user authentication.
 * 
 * This is where you implement your username/password verification logic.
 * Spring Security will call loadUserByUsername() during authentication.
 * 
 * TO CUSTOMIZE:
 * Replace the validateCredentials() method with your actual authentication
 * logic
 * (e.g., database lookup, LDAP, external API, etc.)
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final AccountRepository accountRepository;

    public CustomUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Load user by username.
     * Called by Spring Security during authentication.
     * 
     * @param username the username from the HTTP Basic Auth header
     * @return UserDetails containing username, encoded password, and authorities
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Validate credentials using your custom logic
        logger.info("Authenticating user: {}", username);

        var account = accountRepository.findByUsername(username);

        if (!account.isPresent()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // Return UserDetails with encoded password
        // Spring Security will compare this with the provided password
        return User.builder()
                .username(username)
                .password(account.get().getPasswordHash())
                .roles(account.get().getAccountType().getName()) // You can customize roles/authorities here
                .build();
    }

}
