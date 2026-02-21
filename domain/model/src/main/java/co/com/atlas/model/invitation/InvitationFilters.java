package co.com.atlas.model.invitation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Filter criteria for querying invitation history.
 * All fields are optional; null values are ignored in queries.
 */
@Getter
@Builder
@AllArgsConstructor
public class InvitationFilters {
    
    /**
     * Filter by invitation type (OWNER_SELF_REGISTER, RESIDENT_INVITE, etc.)
     */
    private final InvitationType type;
    
    /**
     * Filter by invitation status (PENDING, ACCEPTED, EXPIRED, CANCELLED)
     */
    private final InvitationStatus status;
    
    /**
     * Filter by unit ID
     */
    private final Long unitId;
    
    /**
     * Free-text search term (matched against email, names, etc.)
     */
    private final String search;
    
    /**
     * Start of date range filter (inclusive)
     */
    private final Instant dateFrom;
    
    /**
     * End of date range filter (inclusive)
     */
    private final Instant dateTo;
}
