package com.matjazt.netmon2.security;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Utility for running code with system-level authentication.
 *
 * <p>Use this in scheduled tasks, background jobs, or internal operations that need to bypass
 * user-level authorization checks.
 *
 * <p>Example usage in a @Scheduled method:
 *
 * <pre>{@code
 * @Scheduled(fixedRate = 60000)
 * public void performSystemTask() {
 *     SystemSecurityContext.runAsSystem(() -> {
 *         // This code runs with ROLE_system
 *         // Can call service methods protected with @PreAuthorize
 *         deviceService.updateDeviceStatuses();
 *     });
 * }
 * }</pre>
 */
@Slf4j
public final class SystemSecurityContext {

    private static final String SYSTEM_USERNAME = "SYSTEM";
    public static final String SYSTEM_ROLE = "ROLE_system";
    public static final String ADMIN_ROLE = "ROLE_admin";

    private SystemSecurityContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Execute a task with system authentication.
     *
     * <p>Sets SecurityContext with ROLE_system for the duration of the task, then restores the
     * previous context.
     *
     * @param task the task to execute
     */
    public static void runAsSystem(Runnable task) {
        SecurityContext previousContext = SecurityContextHolder.getContext();
        try {
            setSystemAuthentication();
            logger.debug("Executing task as SYSTEM");
            task.run();
        } finally {
            SecurityContextHolder.setContext(previousContext);
            logger.debug("Restored previous SecurityContext");
        }
    }

    /**
     * Execute a task with system authentication and return a result.
     *
     * <p>Useful for operations that need to return values.
     *
     * @param <T> return type
     * @param task the task to execute
     * @return the result of the task
     */
    public static <T> T callAsSystem(java.util.concurrent.Callable<T> task) throws Exception {
        SecurityContext previousContext = SecurityContextHolder.getContext();
        try {
            setSystemAuthentication();
            logger.debug("Calling task as SYSTEM");
            return task.call();
        } finally {
            SecurityContextHolder.setContext(previousContext);
            logger.debug("Restored previous SecurityContext");
        }
    }

    /**
     * Set system authentication in the current SecurityContext.
     *
     * <p>Creates an Authentication with username "SYSTEM" and authority "ROLE_system".
     */
    private static void setSystemAuthentication() {
        Authentication systemAuth =
                new UsernamePasswordAuthenticationToken(
                        SYSTEM_USERNAME, null, List.of(new SimpleGrantedAuthority(SYSTEM_ROLE)));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(systemAuth);
        SecurityContextHolder.setContext(context);
    }

    /**
     * Check if the current authentication is the system user.
     *
     * @return true if current authentication is SYSTEM
     */
    public static boolean isSystemAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && SYSTEM_USERNAME.equals(auth.getName())
                && auth.getAuthorities().stream()
                        .anyMatch(a -> SYSTEM_ROLE.equals(a.getAuthority()));
    }
}
