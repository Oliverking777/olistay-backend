package olistay.backend.dto;

import jakarta.validation.constraints.*;
import olistay.backend.enums.InfraZone;
import olistay.backend.enums.PropertyType;
import olistay.backend.enums.TitleType;

/**
 * Payload for POST /properties (create) and PUT /properties/{id} (full update).
 *
 * All ML-engine fields are included so a HOST can supply the full feature
 * set at listing time. Commercial fields (road_frontage_m, shopfront_quality,
 * loading_bay, standby_power_kva) default to zero/false for residential
 * listings and are only meaningful for SHOP, STORE, OFFICE, WAREHOUSE types.
 */
public record PropertyRequestDTO(

        // ── Listing meta ─────────────────────────────────────────────────────
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
        String title,

        String description,

        // ── Location ─────────────────────────────────────────────────────────
        @NotBlank(message = "Neighbourhood is required")
        String neighbourhood,

        @NotBlank(message = "City is required")
        String city,

        Double gpsLat,
        Double gpsLon,

        @NotNull(message = "Infrastructure zone is required")
        InfraZone infraZone,

        // ── Property type ─────────────────────────────────────────────────────
        @NotNull(message = "Property type is required")
        PropertyType propertyType,

        @NotBlank(message = "Unit type is required")
        String unitType,

        // ── Dimensions ───────────────────────────────────────────────────────
        @Positive(message = "Length must be positive")
        Double lengthM,

        @Positive(message = "Width must be positive")
        Double widthM,

        // ── Rooms ─────────────────────────────────────────────────────────────
        @Min(value = 0, message = "Bedroom count cannot be negative")
        Integer numBedrooms,

        @Min(value = 0, message = "Bathroom count cannot be negative")
        Integer numBathrooms,

        @Min(value = 0, message = "Floor level cannot be negative")
        Integer floorLevel,

        Boolean sharedWc,

        // ── Universal amenities ───────────────────────────────────────────────
        Boolean hasParking,
        Boolean hasGenerator,
        Boolean hasWaterMeter,
        Boolean fiberInternet,
        Boolean securityGate,
        Boolean hasGardien,

        // ── Commercial amenities ──────────────────────────────────────────────
        Double roadFrontageM,

        @Min(0) @Max(5)
        Integer shopfrontQuality,

        Boolean loadingBay,

        @PositiveOrZero
        Double standbyPowerKva,

        // ── Proximity ─────────────────────────────────────────────────────────
        Boolean nearSchool,
        Boolean nearMarket,
        Boolean nearHospital,
        Boolean nearHighway,
        Boolean nearUniversity,

        // ── Quality / age / risk ──────────────────────────────────────────────
        @Min(1) @Max(10)
        Integer structuralQuality,

        @Min(1) @Max(10)
        Integer conditionScore,

        @Min(1900) @Max(2100)
        Integer buildYear,

        Boolean floodRisk,

        @Min(1) @Max(10)
        Integer noiseLevel,

        // ── Legal / contractual ───────────────────────────────────────────────
        TitleType titleType,

        @Min(value = 1, message = "Advance months must be at least 1")
        @Max(value = 12, message = "Advance months cannot exceed 12")
        Integer advanceMonths,

        @Min(value = 0, message = "Caution months cannot be negative")
        @Max(value = 6, message = "Caution months cannot exceed 6")
        Integer cautionMonths,

        // ── Pricing ───────────────────────────────────────────────────────────
        @Positive(message = "Rent must be a positive amount in XAF")
        Double rentXaf,

        // ── Scoring signals ───────────────────────────────────────────────────
        @Min(1) @Max(10)
        Integer landlordReputation,

        @Min(1) @Max(10)
        Integer leaseSecurity,

        @Min(1) @Max(10)
        Integer transportScore

) {}
