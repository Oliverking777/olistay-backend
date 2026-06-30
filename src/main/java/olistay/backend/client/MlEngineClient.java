package olistay.backend.client;

import lombok.RequiredArgsConstructor;
import olistay.backend.dto.ml.*;
import olistay.backend.exception.MlEngineException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Single point of contact between Spring Boot and the FastAPI AI Engine
 * (main.py). Every call goes through callEngine() so failures surface
 * uniformly as MlEngineException regardless of whether the cause was a
 * connection failure, timeout, or a 4xx/5xx response from FastAPI.
 *
 * Endpoints wrapped:
 *   POST /ml/predict-rent       — predictRent()
 *   POST /scoring/score         — scoreProperty()
 *   POST /recommender/recommend — recommend()
 *   POST /financial/hidden-costs — calculateHiddenCosts()
 *
 * Each Python router is mounted under its own prefix in main.py
 * (app.include_router(rent_router, prefix="/ml", ...), etc.) — the path
 * constants below mirror that exactly.
 */
@Component
@RequiredArgsConstructor
public class MlEngineClient {

    private final RestClient mlEngineRestClient;

    private static final String PREDICT_RENT_PATH = "/ml/predict-rent";
    private static final String SCORE_PATH = "/scoring/score";
    private static final String RECOMMEND_PATH = "/recommender/recommend";
    private static final String HIDDEN_COSTS_PATH = "/financial/hidden-costs";
    private static final String FINANCIAL_PROFILE_PATH = "/financial/profile";

    /**
     * Calls POST /financial/profile — computes the tenant's live
     * affordability figures (max_sustainable_rent, financial_health,
     * emergency_fund_status, etc.) from their stored TenantFinancialProfile
     * input data. Always called fresh, never cached, since the underlying
     * Cameroonian affordability rules can change independently of the data.
     */
    public olistay.backend.dto.ml.FinancialProfileMlResponseDTO calculateFinancialProfile(
            olistay.backend.dto.ml.FinancialProfileMlRequestDTO request
    ) {
        return callEngine(FINANCIAL_PROFILE_PATH, request, olistay.backend.dto.ml.FinancialProfileMlResponseDTO.class);
    }

    /**
     * Calls POST /ml/predict-rent — XGBoost market rent estimate for a
     * property a HOST is creating or editing. Useful as a "suggested rent"
     * hint in the create-listing flow before the HOST commits to a price.
     */
    public RentPredictResponseDTO predictRent(RentPredictRequestDTO request) {
        return callEngine(PREDICT_RENT_PATH, request, RentPredictResponseDTO.class);
    }

    /**
     * Calls POST /scoring/score — six-dimension optimality score for a
     * single tenant/property pair.
     */
    public ScoringResponseDTO scoreProperty(ScoringRequestDTO request) {
        return callEngine(SCORE_PATH, request, ScoringResponseDTO.class);
    }

    /**
     * Calls POST /recommender/recommend — the full four-stage hybrid
     * pipeline (knowledge filter → content-based → collaborative →
     * scoring/narration delegated downstream). Heavier call; uses the
     * longer read timeout configured in MlEngineConfig.
     */
    public RecommendResponseDTO recommend(RecommendRequestDTO request) {
        return callEngine(RECOMMEND_PATH, request, RecommendResponseDTO.class);
    }

    /**
     * Calls POST /financial/hidden-costs — total cost of occupancy
     * (rent + utilities + transport + advance/caution) for a property
     * against a tenant's income.
     */
    public HiddenCostsResponseDTO calculateHiddenCosts(HiddenCostsRequestDTO request) {
        return callEngine(HIDDEN_COSTS_PATH, request, HiddenCostsResponseDTO.class);
    }

    /**
     * Shared call path: POST {body} to {path}, deserialize as {responseType},
     * translate any failure into MlEngineException.
     *
     * RestClientResponseException covers non-2xx responses (4xx/5xx) and
     * carries the FastAPI error body, which is surfaced in the exception
     * message for easier debugging — FastAPI's HTTPException(detail=...)
     * pattern means the body is usually human-readable already.
     *
     * RestClientException (its parent) covers connection failures, timeouts,
     * and DNS/SSL issues — anything where the engine never responded at all.
     */
    private <T> T callEngine(String path, Object requestBody, Class<T> responseType) {
        try {
            T response = mlEngineRestClient.post()
                    .uri(path)
                    .body(requestBody)
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                throw new MlEngineException(
                        "AI Engine returned an empty response from " + path
                );
            }
            return response;

        } catch (RestClientResponseException e) {
            throw new MlEngineException(
                    "AI Engine call to " + path + " failed with status "
                            + e.getStatusCode() + ": " + e.getResponseBodyAsString(),
                    e
            );
        } catch (RestClientException e) {
            throw new MlEngineException(
                    "AI Engine call to " + path + " failed: " + e.getMessage(),
                    e
            );
        }
    }
}