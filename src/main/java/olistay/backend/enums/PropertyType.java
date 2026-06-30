package olistay.backend.enums;

/**
 * Supported property types on OLISTAY.
 *
 * Order and values match the ML engine's PROPERTY_TYPE_ORDER list in
 * ml_models/rent_predictor.py exactly — do NOT rename or reorder without
 * updating the Python ordinal encoder accordingly.
 *
 * STUDIO, APARTMENT, HOUSE cover residential lettings.
 * LAND covers undeveloped plots.
 * SHOP, STORE, OFFICE, WAREHOUSE cover commercial lettings.
 */
public enum PropertyType {
    STUDIO,
    APARTMENT,
    HOUSE,
    LAND,
    SHOP,
    STORE,
    OFFICE,
    WAREHOUSE
}