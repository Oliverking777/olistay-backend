package olistay.backend.service.impl;

import lombok.RequiredArgsConstructor;
import olistay.backend.client.MlEngineClient;
import olistay.backend.dto.*;
import olistay.backend.dto.ml.FinancialProfileMlRequestDTO;
import olistay.backend.dto.ml.FinancialProfileMlResponseDTO;
import olistay.backend.entity.AvailableFundsBreakdown;
import olistay.backend.entity.ExpenseBreakdown;
import olistay.backend.entity.IncomeSource;
import olistay.backend.entity.TenantFinancialProfile;
import olistay.backend.entity.User;
import olistay.backend.exception.FinancialProfileAlreadyExistsException;
import olistay.backend.exception.ResourceNotFoundException;
import olistay.backend.repository.TenantFinancialProfileRepository;
import olistay.backend.repository.UserRepository;
import olistay.backend.service.TenantFinancialProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantFinancialProfileServiceImpl implements TenantFinancialProfileService {

    private final TenantFinancialProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final MlEngineClient mlEngineClient;

    @Override
    @Transactional
    public TenantFinancialProfileResponseDTO createProfile(String userEmail, TenantFinancialProfileRequestDTO request) {
        User user = findUserOrThrow(userEmail);

        if (profileRepository.existsByUser(user)) {
            throw new FinancialProfileAlreadyExistsException(
                    "A financial profile already exists for this account. Use update instead."
            );
        }

        TenantFinancialProfile profile = TenantFinancialProfile.builder()
                .user(user)
                .build();

        applyRequestToProfile(request, profile);

        TenantFinancialProfile saved = profileRepository.save(profile);
        attachIncomeSources(saved, request.additionalIncomeSources());

        return TenantFinancialProfileResponseDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public TenantFinancialProfileResponseDTO updateProfile(String userEmail, TenantFinancialProfileRequestDTO request) {
        User user = findUserOrThrow(userEmail);

        TenantFinancialProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No financial profile exists yet. Create one first."
                ));

        applyRequestToProfile(request, profile);

        // Replace income sources wholesale on update — simpler and safer
        // than diffing the existing list against the incoming one, and
        // orphanRemoval=true on the collection means stale rows are
        // cleaned up automatically.
        profile.getAdditionalIncomeSources().clear();
        TenantFinancialProfile saved = profileRepository.save(profile);
        attachIncomeSources(saved, request.additionalIncomeSources());

        return TenantFinancialProfileResponseDTO.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantFinancialProfileResponseDTO getProfile(String userEmail) {
        User user = findUserOrThrow(userEmail);
        TenantFinancialProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("No financial profile found for this account"));
        return TenantFinancialProfileResponseDTO.fromEntity(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialProfileMlResponseDTO computeProfile(String userEmail) {
        User user = findUserOrThrow(userEmail);
        TenantFinancialProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("No financial profile found for this account"));

        FinancialProfileMlRequestDTO mlRequest = toMlRequest(user, profile);
        return mlEngineClient.calculateFinancialProfile(mlRequest);
    }

    @Override
    @Transactional
    public void deleteProfile(String userEmail) {
        User user = findUserOrThrow(userEmail);
        TenantFinancialProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("No financial profile found for this account"));
        profileRepository.delete(profile);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void applyRequestToProfile(TenantFinancialProfileRequestDTO request, TenantFinancialProfile profile) {
        profile.setMonthlyIncome(request.monthlyIncome());
        profile.setSavingsGoal(orDefault(request.savingsGoal(), 0.0));
        profile.setIncomeStability(request.incomeStability());
        profile.setJobSector(request.jobSector());
        profile.setEmployerName(request.employerName());
        profile.setJobTitle(request.jobTitle());
        profile.setCurrentCity(request.currentCity());
        profile.setCurrentNeighbourhood(request.currentNeighbourhood());
        profile.setGpsLat(request.gpsLat());
        profile.setGpsLon(request.gpsLon());
        profile.setHouseholdSize(orDefault(request.householdSize(), 1));
        profile.setHasDependents(orDefault(request.hasDependents(), false));
        profile.setNumDependents(orDefault(request.numDependents(), 0));
        profile.setNumRoommates(orDefault(request.numRoommates(), 0));
        profile.setSharesHousingCosts(orDefault(request.sharesHousingCosts(), false));
        profile.setFixedObligations(orDefault(request.fixedObligations(), 0.0));
        profile.setGoalTimelineMonths(orDefault(request.goalTimelineMonths(), 12));
        profile.setCurrentSavings(orDefault(request.currentSavings(), 0.0));
        profile.setHasFinancialEmergency(orDefault(request.hasFinancialEmergency(), false));
        profile.setNeedsParking(orDefault(request.needsParking(), false));
        profile.setNeedsSchoolNearby(orDefault(request.needsSchoolNearby(), false));
        profile.setNeedsHospitalNearby(orDefault(request.needsHospitalNearby(), false));
        profile.setNeedsGenerator(orDefault(request.needsGenerator(), false));

        profile.setExpenseBreakdown(
                request.expenseBreakdown() == null ? null : ExpenseBreakdown.builder()
                        .housingUtilities(orDefault(request.expenseBreakdown().housingUtilities(), 0.0))
                        .foodHouseholdSupplies(orDefault(request.expenseBreakdown().foodHouseholdSupplies(), 0.0))
                        .transportation(orDefault(request.expenseBreakdown().transportation(), 0.0))
                        .personalHealthInsurance(orDefault(request.expenseBreakdown().personalHealthInsurance(), 0.0))
                        .debtRepayments(orDefault(request.expenseBreakdown().debtRepayments(), 0.0))
                        .dependentsSupport(orDefault(request.expenseBreakdown().dependentsSupport(), 0.0))
                        .other(orDefault(request.expenseBreakdown().other(), 0.0))
                        .build()
        );

        profile.setAvailableFundsBreakdown(
                request.availableFundsBreakdown() == null ? null : AvailableFundsBreakdown.builder()
                        .checkingAccount(orDefault(request.availableFundsBreakdown().checkingAccount(), 0.0))
                        .savingsAccount(orDefault(request.availableFundsBreakdown().savingsAccount(), 0.0))
                        .cashOnHand(orDefault(request.availableFundsBreakdown().cashOnHand(), 0.0))
                        .mobileMoney(orDefault(request.availableFundsBreakdown().mobileMoney(), 0.0))
                        .other(orDefault(request.availableFundsBreakdown().other(), 0.0))
                        .build()
        );
    }

    private void attachIncomeSources(TenantFinancialProfile profile, List<IncomeSourceDTO> sources) {
        if (sources == null || sources.isEmpty()) return;

        List<IncomeSource> entities = new ArrayList<>();
        for (IncomeSourceDTO dto : sources) {
            entities.add(IncomeSource.builder()
                    .financialProfile(profile)
                    .incomeType(dto.incomeType())
                    .description(dto.description())
                    .monthlyAmount(orDefault(dto.monthlyAmount(), 0.0))
                    .build());
        }
        profile.getAdditionalIncomeSources().addAll(entities);
    }

    /**
     * Builds the FastAPI request DTO from the saved entity. tenant_id is
     * the User's ID (as a String) — FastAPI just echoes it back in the
     * response for correlation, it doesn't look it up anywhere.
     */
    private FinancialProfileMlRequestDTO toMlRequest(User user, TenantFinancialProfile p) {
        List<FinancialProfileMlRequestDTO.IncomeSourceMlDTO> incomeSources =
                p.getAdditionalIncomeSources() == null ? List.of()
                        : p.getAdditionalIncomeSources().stream()
                        .map(s -> new FinancialProfileMlRequestDTO.IncomeSourceMlDTO(
                                s.getIncomeType(), s.getDescription(), s.getMonthlyAmount()
                        ))
                        .toList();

        FinancialProfileMlRequestDTO.ExpenseBreakdownMlDTO expenseMl = p.getExpenseBreakdown() == null ? null
                : new FinancialProfileMlRequestDTO.ExpenseBreakdownMlDTO(
                p.getExpenseBreakdown().getHousingUtilities(),
                p.getExpenseBreakdown().getFoodHouseholdSupplies(),
                p.getExpenseBreakdown().getTransportation(),
                p.getExpenseBreakdown().getPersonalHealthInsurance(),
                p.getExpenseBreakdown().getDebtRepayments(),
                p.getExpenseBreakdown().getDependentsSupport(),
                p.getExpenseBreakdown().getOther()
        );

        FinancialProfileMlRequestDTO.AvailableFundsBreakdownMlDTO fundsMl = p.getAvailableFundsBreakdown() == null
                ? null
                : new FinancialProfileMlRequestDTO.AvailableFundsBreakdownMlDTO(
                p.getAvailableFundsBreakdown().getCheckingAccount(),
                p.getAvailableFundsBreakdown().getSavingsAccount(),
                p.getAvailableFundsBreakdown().getCashOnHand(),
                p.getAvailableFundsBreakdown().getMobileMoney(),
                p.getAvailableFundsBreakdown().getOther()
        );

        return new FinancialProfileMlRequestDTO(
                String.valueOf(user.getId()),
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
                expenseMl,
                p.getGoalTimelineMonths(),
                p.getCurrentSavings(),
                fundsMl,
                p.getHasFinancialEmergency()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public double resolveEffectiveMonthlyIncome(String userEmail) {
        return resolveEffectiveMonthlyIncomeFromEntity(getProfileEntity(userEmail));
    }

    @Override
    @Transactional(readOnly = true)
    public double resolveEffectiveFixedObligations(String userEmail) {
        return resolveEffectiveFixedObligationsFromEntity(getProfileEntity(userEmail));
    }

    @Override
    @Transactional(readOnly = true)
    public double resolveEffectiveCurrentSavings(String userEmail) {
        return resolveEffectiveCurrentSavingsFromEntity(getProfileEntity(userEmail));
    }

    @Override
    @Transactional(readOnly = true)
    public TenantFinancialProfile getProfileEntity(String userEmail) {
        User user = findUserOrThrow(userEmail);
        return profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("No financial profile found for this account"));
    }

    /**
     * Resolves the EFFECTIVE monthly income — base monthlyIncome plus the
     * sum of all additionalIncomeSources. Needed because the scoring
     * (/scoring/score) and recommender (/recommender/recommend) endpoints'
     * TenantData/PipelineTenant models only accept a single flat
     * monthly_income field with no additional_income_sources support,
     * unlike /financial/profile. Without this resolution, a tenant with
     * itemised side income would silently have it dropped when scored or
     * recommended against, even though it's respected in their financial
     * profile.
     */
    private double resolveEffectiveMonthlyIncomeFromEntity(TenantFinancialProfile profile) {
        double additional = profile.getAdditionalIncomeSources() == null ? 0.0
                : profile.getAdditionalIncomeSources().stream()
                .mapToDouble(s -> s.getMonthlyAmount() != null ? s.getMonthlyAmount() : 0.0)
                .sum();
        return (profile.getMonthlyIncome() != null ? profile.getMonthlyIncome() : 0.0) + additional;
    }

    /**
     * Resolves the EFFECTIVE fixed obligations — expenseBreakdown.total()
     * if present, otherwise the flat fixedObligations figure. Same rationale
     * as resolveEffectiveMonthlyIncomeFromEntity(): the scoring/recommender
     * endpoints don't support the itemised override the way
     * /financial/profile does.
     */
    private double resolveEffectiveFixedObligationsFromEntity(TenantFinancialProfile profile) {
        if (profile.getExpenseBreakdown() != null) {
            return profile.getExpenseBreakdown().total();
        }
        return profile.getFixedObligations() != null ? profile.getFixedObligations() : 0.0;
    }

    /**
     * Resolves the EFFECTIVE current savings — availableFundsBreakdown.total()
     * if present, otherwise the flat currentSavings figure. Same override
     * rationale as the other two resolve*FromEntity methods.
     */
    private double resolveEffectiveCurrentSavingsFromEntity(TenantFinancialProfile profile) {
        if (profile.getAvailableFundsBreakdown() != null) {
            return profile.getAvailableFundsBreakdown().total();
        }
        return profile.getCurrentSavings() != null ? profile.getCurrentSavings() : 0.0;
    }

    private <T> T orDefault(T value, T fallback) {
        return value != null ? value : fallback;
    }
}