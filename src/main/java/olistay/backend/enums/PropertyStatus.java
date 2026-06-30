package olistay.backend.enums;

/**
 * Lifecycle status of a property listing.
 *
 * AVAILABLE   — actively listed, accepting enquiries
 * OCCUPIED    — currently tenanted, not available
 * UNDER_REVIEW — submitted by HOST, pending admin approval before going live
 * ARCHIVED    — delisted by HOST or removed by ADMIN
 */
public enum PropertyStatus {
    AVAILABLE,
    OCCUPIED,
    UNDER_REVIEW,
    ARCHIVED
}
