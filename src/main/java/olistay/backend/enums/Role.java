package olistay.backend.enums;

/**
 * Represents the role assigned to a {@link olistay.backend.entity.User}.
 *
 * GUEST  - default role on registration; can browse and search properties.
 * HOST   - a guest who has completed the "Become a Landlord" flow; can list properties.
 * ADMIN  - platform administrator; full access.
 */
public enum Role {
    GUEST,
    HOST,
    ADMIN
}
