package olistay.backend.dto;

/**
 * Payload for POST /admin/listings/{id}/reject.
 * reason is optional — admin can reject without a detailed reason,
 * though providing one is strongly encouraged for host UX.
 */
public record RejectionRequestDTO(String reason) {}