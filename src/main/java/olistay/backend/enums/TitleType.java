package olistay.backend.enums;

/**
 * Land/property title security classification for Cameroon.
 *
 * Maps to TITLE_TYPE_ORDER in ml_models/rent_predictor.py:
 *   ["none", "occupation", "foncier"] → ordinal 0–2
 * and to TITLE_SECURITY_VALUES in recommender/content_based.py:
 *   {"foncier": 1.0, "occupation": 0.5, "none": 0.0}
 *
 * FONCIER    — formal land title (titre foncier), highest security
 * OCCUPATION — occupation permit, intermediate security
 * NONE       — no formal title, informal arrangement
 */
public enum TitleType {
    NONE,
    OCCUPATION,
    FONCIER
}
