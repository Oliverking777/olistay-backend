package olistay.backend.enums;

/**
 * Lifecycle states of a property visit appointment.
 *
 * PENDING   — tenant has requested a visit; awaiting host action.
 * CONFIRMED — host has accepted the visit at the scheduled time.
 * CANCELLED — either party cancelled before the visit took place.
 */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
