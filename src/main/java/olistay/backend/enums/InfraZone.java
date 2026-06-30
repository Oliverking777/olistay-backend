package olistay.backend.enums;

/**
 * MINDCAF infrastructure zone classification (Zone I–V).
 * Zone I is the best-serviced (highest quality), Zone V the least.
 *
 * Maps to INFRA_ZONE_ORDER in ml_models/rent_predictor.py:
 *   ["V", "IV", "III", "II", "I"] → ordinal 0–4
 * and to INFRA_ZONE_VALUES in recommender/content_based.py:
 *   {"I": 1.0, "II": 0.8, "III": 0.6, "IV": 0.4, "V": 0.2}
 *
 * Store as STRING in the DB so the zone label is human-readable.
 */
public enum InfraZone {
    I,    // best — fully serviced: tarred roads, mains water, reliable power
    II,
    III,  // median — mixed services
    IV,
    V     // lowest — unpaved, shared/absent utilities
}
