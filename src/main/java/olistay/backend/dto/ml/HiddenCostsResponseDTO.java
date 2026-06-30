package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to HiddenCostsResponse in financial/hidden_costs.py.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HiddenCostsResponseDTO(

        @JsonProperty("property_id")
        String propertyId,

        @JsonProperty("neighbourhood")
        String neighbourhood,

        @JsonProperty("breakdown")
        CostBreakdownDTO breakdown,

        @JsonProperty("tco_to_income_ratio")
        Double tcoToIncomeRatio,

        @JsonProperty("tco_burden")
        String tcoBurden,

        @JsonProperty("advance_months")
        Integer advanceMonths,

        @JsonProperty("caution_months")
        Integer cautionMonths,

        @JsonProperty("neighbourhood_found")
        Boolean neighbourhoodFound,

        @JsonProperty("summary")
        String summary

) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CostBreakdownDTO(

            @JsonProperty("rent")
            Double rent,

            @JsonProperty("water")
            Double water,

            @JsonProperty("electricity")
            Double electricity,

            @JsonProperty("generator_contrib")
            Double generatorContrib,

            @JsonProperty("building_charges")
            Double buildingCharges,

            @JsonProperty("gardien_contrib")
            Double gardienContrib,

            @JsonProperty("transport")
            Double transport,

            @JsonProperty("transport_delta")
            Double transportDelta,

            @JsonProperty("total_monthly_cost")
            Double totalMonthlyCost,

            @JsonProperty("advance_payment")
            Double advancePayment,

            @JsonProperty("caution_payment")
            Double cautionPayment,

            @JsonProperty("total_upfront_cost")
            Double totalUpfrontCost

    ) {}
}
