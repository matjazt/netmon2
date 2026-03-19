package com.matjazt.netmon2.security;

import com.matjazt.netmon2.repository.AccountNetworkRepository;
import com.matjazt.netmon2.repository.AccountRepository;
import com.matjazt.netmon2.repository.DeviceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
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
 * {@literal @}networkAuthorizationService.canAccessNetwork(authentication, #networkId)")
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NetworkAuthorizationService {

    private final AccountRepository accountRepository;
    private final AccountNetworkRepository accountNetworkRepository;
    private final DeviceRepository deviceRepository;
    // Inject the proxied self so that @Cacheable works when called from the same class
    private final ObjectProvider<NetworkAuthorizationService>
            self; // proxy for caching/transactions

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
            key = "'n' + #networkId + '_' + #authentication.name",
            sync = true)
    public boolean canAccessNetwork(Authentication authentication, Long networkId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Access denied: no authenticated user");
            return false;
        }

        String username = authentication.getName();
        log.debug("Checking network access for user '{}' to network ID {}", username, networkId);

        if (hasAdminOrSystemRole(authentication)) {
            log.debug("Access granted: user '{}' has admin or system role", username);
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
                                log.debug(
                                        "Access granted: user '{}' has explicit access to"
                                                + " network {}",
                                        username,
                                        networkId);
                            } else {
                                log.debug(
                                        "Access denied: user '{}' does not have access to"
                                                + " network {}",
                                        username,
                                        networkId);
                            }
                            return hasAccess;
                        })
                .orElseGet(
                        () -> {
                            log.debug(
                                    "Access denied: authenticated user '{}' not found in"
                                            + " database",
                                    username);
                            return false;
                        });
    }

    /**
     * Check if the authenticated user can access a specific device.
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
     * @param deviceId ID of the device to check access for
     * @return true if user has access, false otherwise
     */
    @Cacheable(
            cacheNames = "networkAccessCache",
            key = "'d' + #deviceId + '_' + #authentication.name",
            sync = true)
    public boolean canAccessDevice(Authentication authentication, Long deviceId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Access denied: no authenticated user");
            return false;
        }

        if (hasAdminOrSystemRole(authentication)) {
            log.debug(
                    "Access granted: user '{}' has admin or system role", authentication.getName());
            return true;
        }

        var deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            log.debug("Access denied: device ID {} not found", deviceId);
            return false;
        }

        var networkId = deviceOpt.get().getNetwork().getId();
        return self.getObject().canAccessNetwork(authentication, networkId);
    }

    private boolean hasAdminOrSystemRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(
                        role ->
                                role.equals(SystemSecurityContext.ADMIN_ROLE)
                                        || role.equals(SystemSecurityContext.SYSTEM_ROLE));
    }
}
