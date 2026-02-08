package co.com.atlas.tenant;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local context holder for multi-tenant information.
 * <p>
 * This class provides a centralized way to store and retrieve tenant-specific
 * information (organization ID and user ID) for the current request context.
 * It uses ThreadLocal to ensure thread safety in reactive environments.
 * </p>
 * 
 * <p><strong>Usage:</strong></p>
 * <pre>
 * // Set tenant context (typically done in a WebFilter)
 * TenantContext.setOrganizationId(123L);
 * TenantContext.setUserId(456L);
 * 
 * // Retrieve tenant context (in repositories, services, etc.)
 * Long orgId = TenantContext.getOrganizationId();
 * Long userId = TenantContext.getUserId();
 * 
 * // Clear context (mandatory at the end of request)
 * TenantContext.clear();
 * </pre>
 * 
 * <p><strong>Critical:</strong> Always call {@link #clear()} in a finally block
 * or doFinally() to prevent memory leaks and context pollution between requests.</p>
 * 
 * @see co.com.atlas.api.config.TenantFilter
 * @author Atlas Platform Team
 * @since 2026-02-07
 */
@Slf4j
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_ORGANIZATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    // Private constructor to prevent instantiation
    private TenantContext() {
        throw new IllegalStateException("Utility class - do not instantiate");
    }

    /**
     * Sets the organization ID for the current thread/request context.
     * 
     * @param organizationId the organization ID to set (must not be null)
     * @throws IllegalArgumentException if organizationId is null
     */
    public static void setOrganizationId(Long organizationId) {
        if (organizationId == null) {
            log.warn("Attempted to set null organization ID in TenantContext");
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        CURRENT_ORGANIZATION_ID.set(organizationId);
        log.debug("TenantContext: Set organizationId={}", organizationId);
    }

    /**
     * Retrieves the organization ID for the current thread/request context.
     * 
     * @return the current organization ID, or null if not set
     */
    public static Long getOrganizationId() {
        return CURRENT_ORGANIZATION_ID.get();
    }

    /**
     * Retrieves the organization ID or throws an exception if not set.
     * <p>
     * Use this method in critical paths where tenant isolation is mandatory.
     * </p>
     * 
     * @return the current organization ID (never null)
     * @throws IllegalStateException if organization ID is not set in context
     */
    public static Long getOrganizationIdOrThrow() {
        Long organizationId = CURRENT_ORGANIZATION_ID.get();
        if (organizationId == null) {
            log.error("TenantContext: Organization ID not found in context - possible security breach!");
            throw new IllegalStateException("Organization ID not set in TenantContext. Request not authenticated?");
        }
        return organizationId;
    }

    /**
     * Sets the user ID for the current thread/request context.
     * 
     * @param userId the user ID to set (must not be null)
     * @throws IllegalArgumentException if userId is null
     */
    public static void setUserId(Long userId) {
        if (userId == null) {
            log.warn("Attempted to set null user ID in TenantContext");
            throw new IllegalArgumentException("User ID cannot be null");
        }
        CURRENT_USER_ID.set(userId);
        log.debug("TenantContext: Set userId={}", userId);
    }

    /**
     * Retrieves the user ID for the current thread/request context.
     * 
     * @return the current user ID, or null if not set
     */
    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }

    /**
     * Retrieves the user ID or throws an exception if not set.
     * 
     * @return the current user ID (never null)
     * @throws IllegalStateException if user ID is not set in context
     */
    public static Long getUserIdOrThrow() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) {
            log.error("TenantContext: User ID not found in context");
            throw new IllegalStateException("User ID not set in TenantContext. Request not authenticated?");
        }
        return userId;
    }

    /**
     * Checks if the tenant context is fully initialized.
     * 
     * @return true if both organization ID and user ID are set, false otherwise
     */
    public static boolean isInitialized() {
        return CURRENT_ORGANIZATION_ID.get() != null && CURRENT_USER_ID.get() != null;
    }

    /**
     * Clears all tenant context data from the current thread.
     * <p>
     * <strong>CRITICAL:</strong> This method MUST be called at the end of every request
     * to prevent memory leaks and context pollution. Typically called in a WebFilter's
     * doFinally() block.
     * </p>
     */
    public static void clear() {
        Long orgId = CURRENT_ORGANIZATION_ID.get();
        Long userId = CURRENT_USER_ID.get();
        
        CURRENT_ORGANIZATION_ID.remove();
        CURRENT_USER_ID.remove();
        
        log.debug("TenantContext: Cleared context (orgId={}, userId={})", orgId, userId);
    }

    /**
     * Returns a string representation of the current tenant context.
     * <p>
     * Useful for logging and debugging purposes.
     * </p>
     * 
     * @return a string describing the current tenant context
     */
    public static String getContextInfo() {
        return String.format("TenantContext[organizationId=%d, userId=%d, initialized=%b]",
                CURRENT_ORGANIZATION_ID.get(),
                CURRENT_USER_ID.get(),
                isInitialized());
    }
}
