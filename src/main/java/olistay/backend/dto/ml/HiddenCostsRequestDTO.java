package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to HiddenCostsRequest in financial/hidden_costs.py.
 * POST /financial/hidden-costs.
 */
public record HiddenCostsRequestDTO(

        @JsonProperty("property_id")
        String propertyId,

        @JsonProperty("rent")
        Double rent,

        @JsonProperty("neighbourhood")
        String neighbourhood,

        @JsonProperty("has_generator")
        Boolean hasGenerator,

        @JsonProperty("has_water_meter")
        Boolean hasWaterMeter,

        @JsonProperty("advance_months")
        Integer advanceMonths,

        @JsonProperty("caution_months")
        Integer cautionMonths,

        @JsonProperty("has_gardien")
        Boolean hasGardien,

        @JsonProperty("tenant_current_neighbourhood")
        String tenantCurrentNeighbourhood,

        @JsonProperty("tenant_monthly_income")
        Double tenantMonthlyIncome

) {
    /**
     * Builds a hidden-costs request from the ML feature DTO plus the tenant
     * context that isn't part of the property's own data (income, current
     * neighbourhood for transport delta calculation).
     */
    public static HiddenCostsRequestDTO fromMlFeatures(
            PropertyMlFeaturesDTO f,
            String tenantCurrentNeighbourhood,
            Double tenantMonthlyIncome
    ) {
        return new HiddenCostsRequestDTO(
                f.propertyId(),
                f.rent(),
                f.neighbourhood(),
                f.hasGenerator(),
                f.hasWaterMeter(),
                f.advanceMonths(),
                f.cautionMonths(),
                f.hasGardien(),
                tenantCurrentNeighbourhood,
                tenantMonthlyIncome
        );
    }
}