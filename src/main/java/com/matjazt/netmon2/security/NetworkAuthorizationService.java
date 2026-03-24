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
 * <p>Used in @PreAuthorize expressions to check if a user has read or write permission to a
 * specific network, device, or account.
 *
 * <p>Viewer role has read-only access; user role has read and write access.
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

    // ========== NETWORK ==========

    /**
     * Check if the authenticated user can READ a specific network. Admin, system, user, and viewer
     * roles are eligible; user/viewer require explicit membership.
     */
    @Cacheable(
            cacheNames = "networkAccessCache",
            key = "'rn' + #networkId + '_' + #authentication.name",
            sync = true)
    public boolean canReadNetwork(Authentication authentication, Long networkId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Access denied: no authenticated user");
            return false;
        }
        if (hasAdminOrSystemRole(authentication)) return true;
        if (!hasUserOrViewerRole(authentication)) {
            log.debug(
                    "Access denied: user '{}' does not have a recognised role",
                    authentication.getName());
            return false;
        }
        return checkNetworkMembership(authentication.getName(), networkId);
    }

    /**
     * Check if the authenticated user can WRITE to a specific network. Admin and system roles are
     * always allowed; user role requires explicit membership. Viewer role is denied.
     */
    @Cacheable(
            cacheNames = "networkAccessCache",
            key = "'wn' + #networkId + '_' + #authentication.name",
            sync = true)
    public boolean canWriteNetwork(Authentication authentication, Long networkId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Access denied: no authenticated user");
            return false;
        }
        if (hasAdminOrSystemRole(authentication)) return true;
        if (!hasUserRole(authentication)) {
            log.debug(
                    "Access denied: user '{}' does not have write permission (viewer or unknown"
                            + " role)",
                    authentication.getName());
            return false;
        }
        return checkNetworkMembership(authentication.getName(), networkId);
    }

    // ========== DEVICE ==========

    /**
     * Check if the authenticated user can READ a specific device. Resolves the device's network and
     * delegates to {@link #canReadNetwork}.
     */
    @Cacheable(
            cacheNames = "networkAccessCache",
            key = "'rd' + #deviceId + '_' + #authentication.name",
            sync = true)
    public boolean canReadDevice(Authentication authentication, Long deviceId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Access denied: no authenticated user");
            return false;
        }
        if (hasAdminOrSystemRole(authentication)) return true;
        if (!hasUserOrViewerRole(authentication)) {
            log.debug(
                    "Access denied: user '{}' does not have a recognised role",
                    authentication.getName());
            return false;
        }
        var deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            log.debug("Access denied: device ID {} not found", deviceId);
            return false;
        }
        return self.getObject()
                .canReadNetwork(authentication, deviceOpt.get().getNetwork().getId());
    }

    /**
     * Check if the authenticated user can WRITE to a specific device. Resolves the device's network
     * and delegates to {@link #canWriteNetwork}. Viewer role is denied.
     */
    @Cacheable(
            cacheNames = "networkAccessCache",
            key = "'wd' + #deviceId + '_' + #authentication.name",
            sync = true)
    public boolean canWriteDevice(Authentication authentication, Long deviceId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Access denied: no authenticated user");
            return false;
        }
        if (hasAdminOrSystemRole(authentication)) return true;
        if (!hasUserRole(authentication)) {
            log.debug(
                    "Access denied: user '{}' does not have write permission (viewer or unknown"
                            + " role)",
                    authentication.getName());
            return false;
        }
        var deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            log.debug("Access denied: device ID {} not found", deviceId);
            return false;
        }
        return self.getObject()
                .canWriteNetwork(authentication, deviceOpt.get().getNetwork().getId());
    }

    // ========== ACCOUNT ==========

    /**
     * Check if the authenticated user can READ a specific account. Admin/system can read any
     * account; user/viewer can only read their own.
     */
    @Cacheable(
            cacheNames = "networkAccessCache",
            key = "'ra' + #accountId + '_' + #authentication.name",
            sync = true)
    public boolean canReadAccount(Authentication authentication, Long accountId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Access denied: no authenticated user");
            return false;
        }
        if (hasAdminOrSystemRole(authentication)) return true;
        if (!hasUserOrViewerRole(authentication)) {
            log.debug(
                    "Access denied: user '{}' does not have a recognised role",
                    authentication.getName());
            return false;
        }
        return checkOwnAccount(authentication.getName(), accountId);
    }

    /**
     * Check if the authenticated user can WRITE to a specific account. Admin/system can write any
     * account; user can only write their own. Viewer role is denied.
     */
    @Cacheable(
            cacheNames = "networkAccessCache",
            key = "'wa' + #accountId + '_' + #authentication.name",
            sync = true)
    public boolean canWriteAccount(Authentication authentication, Long accountId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Access denied: no authenticated user");
            return false;
        }
        if (hasAdminOrSystemRole(authentication)) return true;
        if (!hasUserRole(authentication)) {
            log.debug(
                    "Access denied: user '{}' does not have write permission (viewer or unknown"
                            + " role)",
                    authentication.getName());
            return false;
        }
        return checkOwnAccount(authentication.getName(), accountId);
    }

    // ========== PRIVATE HELPERS ==========

    private boolean checkNetworkMembership(String username, Long networkId) {
        return accountRepository
                .findByUsername(username)
                .map(
                        account -> {
                            boolean hasAccess =
                                    accountNetworkRepository.existsByAccount_IdAndNetwork_Id(
                                            account.getId(), networkId);
                            log.debug(
                                    "Network membership check: user='{}', networkId={}, result={}",
                                    username,
                                    networkId,
                                    hasAccess);
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

    private boolean checkOwnAccount(String username, Long accountId) {
        return accountRepository
                .findById(accountId)
                .map(
                        account -> {
                            boolean isOwn = account.getUsername().equals(username);
                            log.debug(
                                    "Account ownership check: user='{}', accountId={}, result={}",
                                    username,
                                    accountId,
                                    isOwn);
                            return isOwn;
                        })
                .orElseGet(
                        () -> {
                            log.debug("Access denied: account ID {} not found", accountId);
                            return false;
                        });
    }

    private boolean hasAdminOrSystemRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(
                        role ->
                                role.equals(SystemSecurityContext.ADMIN_ROLE)
                                        || role.equals(SystemSecurityContext.SYSTEM_ROLE));
    }

    private boolean hasUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals(SystemSecurityContext.USER_ROLE));
    }

    private boolean hasViewerRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals(SystemSecurityContext.VIEWER_ROLE));
    }

    private boolean hasUserOrViewerRole(Authentication authentication) {
        return hasUserRole(authentication) || hasViewerRole(authentication);
    }
}
