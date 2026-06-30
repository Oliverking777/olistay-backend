package olistay.backend.service;

import olistay.backend.dto.TenantFinancialProfileRequestDTO;
import olistay.backend.dto.TenantFinancialProfileResponseDTO;
import olistay.backend.dto.ml.FinancialProfileMlResponseDTO;

public interface TenantFinancialProfileService {

    /**
     * Creates the authenticated user's financial profile. Throws if one
     * already exists — use updateProfile() to modify an existing profile.
     */
    TenantFinancialProfileResponseDTO createProfile(String userEmail, TenantFinancialProfileRequestDTO request);

    /**
     * Fully replaces the authenticated user's financial profile data.
     */
    TenantFinancialProfileResponseDTO updateProfile(String userEmail, TenantFinancialProfileRequestDTO request);

    /**
     * Returns the raw saved profile data (not computed affordability figures).
     */
    TenantFinancialProfileResponseDTO getProfile(String userEmail);

    /**
     * Computes and returns live affordability figures by calling FastAPI's
     * /financial/profile with the user's saved profile data. This is the
     * "get my financial profile, show max sustainable rent etc." endpoint.
     */
    FinancialProfileMlResponseDTO computeProfile(String userEmail);

    /**
     * Deletes the authenticated user's financial profile.
     */
    void deleteProfile(String userEmail);

    /**
     * Resolves the EFFECTIVE monthly income (base + additional income
     * sources) for a given user — needed by callers (e.g. PropertyService
     * scoring/recommend) that feed into FastAPI endpoints which don't
     * support the itemised additional_income_sources override, only
     * /financial/profile does. See impl javadoc for full rationale.
     */
    double resolveEffectiveMonthlyIncome(String userEmail);

    /**
     * Resolves the EFFECTIVE fixed obligations (itemised breakdown total,
     * or flat aggregate) for a given user. Same rationale as
     * resolveEffectiveMonthlyIncome().
     */
    double resolveEffectiveFixedObligations(String userEmail);

    /**
     * Resolves the EFFECTIVE current savings (itemised breakdown total, or
     * flat aggregate) for a given user. Same rationale as
     * resolveEffectiveMonthlyIncome().
     */
    double resolveEffectiveCurrentSavings(String userEmail);

    /**
     * Returns the raw TenantFinancialProfile entity for internal use by
     * other services (e.g. PropertyService building scoring/recommend
     * requests) that need more fields than the three resolve* methods
     * expose. Throws ResourceNotFoundException if no profile exists.
     */
    olistay.backend.entity.TenantFinancialProfile getProfileEntity(String userEmail);
}