package olistay.backend.dto;

import olistay.backend.entity.TenantFinancialProfile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Echoes the saved TenantFinancialProfile entity — raw input data only.
 * For the COMPUTED affordability figures (max_sustainable_rent,
 * financial_health, emergency_fund_status, etc.), see
 * FinancialProfileComputedDTO, returned by GET /tenant/financial-profile/compute,
 * which calls FastAPI live rather than reading a stale cached value.
 */
public record TenantFinancialProfileResponseDTO(
        Long id,
        Double monthlyIncome,
        Double savingsGoal,
        List<IncomeSourceDTO> additionalIncomeSources,
        String incomeStability,
        String jobSector,
        String employerName,
        String jobTitle,
        String currentCity,
        String currentNeighbourhood,
        Double gpsLat,
        Double gpsLon,
        Integer householdSize,
        Boolean hasDependents,
        Integer numDependents,
        Integer numRoommates,
        Boolean sharesHousingCosts,
        Double fixedObligations,
        ExpenseBreakdownDTO expenseBreakdown,
        Integer goalTimelineMonths,
        Double currentSavings,
        AvailableFundsBreakdownDTO availableFundsBreakdown,
        Boolean hasFinancialEmergency,
        Boolean needsParking,
        Boolean needsSchoolNearby,
        Boolean needsHospitalNearby,
        Boolean needsGenerator,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TenantFinancialProfileResponseDTO fromEntity(TenantFinancialProfile p) {
        List<IncomeSourceDTO> incomeSources = p.getAdditionalIncomeSources() == null
                ? List.of()
                : p.getAdditionalIncomeSources().stream()
                .map(s -> new IncomeSourceDTO(s.getIncomeType(), s.getDescription(), s.getMonthlyAmount()))
                .toList();

        ExpenseBreakdownDTO expenseDto = p.getExpenseBreakdown() == null ? null : new ExpenseBreakdownDTO(
                p.getExpenseBreakdown().getHousingUtilities(),
                p.getExpenseBreakdown().getFoodHouseholdSupplies(),
                p.getExpenseBreakdown().getTransportation(),
                p.getExpenseBreakdown().getPersonalHealthInsurance(),
                p.getExpenseBreakdown().getDebtRepayments(),
                p.getExpenseBreakdown().getDependentsSupport(),
                p.getExpenseBreakdown().getOther()
        );

        AvailableFundsBreakdownDTO fundsDto = p.getAvailableFundsBreakdown() == null ? null
                : new AvailableFundsBreakdownDTO(
                p.getAvailableFundsBreakdown().getCheckingAccount(),
                p.getAvailableFundsBreakdown().getSavingsAccount(),
                p.getAvailableFundsBreakdown().getCashOnHand(),
                p.getAvailableFundsBreakdown().getMobileMoney(),
                p.getAvailableFundsBreakdown().getOther()
        );

        return new TenantFinancialProfileResponseDTO(
                p.getId(),
                p.getMonthlyIncome(),
                p.getSavingsGoal(),
                incomeSources,
                p.getIncomeStability(),
                p.getJobSector(),
                p.getEmployerName(),
                p.getJobTitle(),
                p.getCurrentCity(),
                p.getCurrentNeighbourhood(),
                p.getGpsLat(),
                p.getGpsLon(),
                p.getHouseholdSize(),
                p.getHasDependents(),
                p.getNumDependents(),
                p.getNumRoommates(),
                p.getSharesHousingCosts(),
                p.getFixedObligations(),
                expenseDto,
                p.getGoalTimelineMonths(),
                p.getCurrentSavings(),
                fundsDto,
                p.getHasFinancialEmergency(),
                p.getNeedsParking(),
                p.getNeedsSchoolNearby(),
                p.getNeedsHospitalNearby(),
                p.getNeedsGenerator(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}