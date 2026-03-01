package com.matjazt.netmon2.security;

import com.matjazt.netmon2.repository.AccountNetworkRepository;
import com.matjazt.netmon2.repository.AccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Authorization service for network-level access control.
 *
 * <p>Used in @PreAuthorize expressions to check if a user has permission to access a specific
 * network.
 *
 * <p>Example usage: @PreAuthorize("hasRole('admin') or hasRole('system') or
 * {@literal @}networkAuthorizationService.canAccess(authentication, #networkId)")
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NetworkAuthorizationService {

    private final AccountRepository accountRepository;
    private final AccountNetworkRepository accountNetworkRepository;

    /**
     * Check if the authenticated user can access a specific network.
     *
     * <p>Authorization rules:
     *
     * <ul>
     *   <li>Admin users can access all networks
     *   <li>System role can access all networks (for scheduled tasks)
     *   <li>Regular users can only access networks explicitly assigned to them
     * </ul>
     *
     * @param authentication current Authentication from Spring Security context
     * @param networkId ID of the network to check access for
     * @return true if user has access, false otherwise
     */
    @Cacheable(
            cacheNames = "networkAccessCache",
            key = "#networkId + '_' + #authentication.name",
            sync = true)
    public boolean canAccess(Authentication authentication, Long networkId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.debug("Access denied: no authenticated user");
            return false;
        }

        String username = authentication.getName();
        logger.debug("Checking network access for user '{}' to network ID {}", username, networkId);

        // Check if user has admin or system role
        boolean hasAdminOrSystemRole =
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(
                                role ->
                                        role.equals(SystemSecurityContext.ADMIN_ROLE)
                                                || role.equals(SystemSecurityContext.SYSTEM_ROLE));

        if (hasAdminOrSystemRole) {
            logger.debug("Access granted: user '{}' has admin or system role", username);
            return true;
        }

        // Check if user has explicit access to this network
        return accountRepository
                .findByUsername(username)
                .map(
                        account -> {
                            boolean hasAccess =
                                    accountNetworkRepository.existsByAccount_IdAndNetwork_Id(
                                            account.getId(), networkId);
                            if (hasAccess) {
                                logger.debug(
                                        "Access granted: user '{}' has explicit access to"
                                                + " network {}",
                                        username,
                                        networkId);
                            } else {
                                logger.debug(
                                        "Access denied: user '{}' does not have access to"
                                                + " network {}",
                                        username,
                                        networkId);
                            }
                            return hasAccess;
                        })
                .orElseGet(
                        () -> {
                            logger.warn(
                                    "Access denied: authenticated user '{}' not found in"
                                            + " database",
                                    username);
                            return false;
                        });
    }

    /**
     * Convenience method for boolean parameters - useful when network ID is passed as a parameter.
     *
     * @param authentication current Authentication
     * @param networkId network ID to check
     * @return true if user has access
     */
    public boolean hasNetworkAccess(Authentication authentication, Long networkId) {
        return canAccess(authentication, networkId);
    }
}
