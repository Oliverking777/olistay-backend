package olistay.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import olistay.backend.dto.TenantFinancialProfileRequestDTO;
import olistay.backend.dto.TenantFinancialProfileResponseDTO;
import olistay.backend.dto.ml.FinancialProfileMlResponseDTO;
import olistay.backend.service.TenantFinancialProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Tenant financial profile endpoints. All routes require authentication —
 * a tenant's financial data is always scoped to the authenticated user via
 * @AuthenticationPrincipal, never accepted as a path/query parameter.
 */
@RestController
@RequestMapping("/tenant/financial-profile")
@RequiredArgsConstructor
public class TenantFinancialProfileController {

    private final TenantFinancialProfileService financialProfileService;

    /**
     * POST /tenant/financial-profile
     * Creates the authenticated user's financial profile. 409 if one
     * already exists — use PUT to update instead.
     */
    @PostMapping
    public ResponseEntity<TenantFinancialProfileResponseDTO> createProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TenantFinancialProfileRequestDTO request
    ) {
        TenantFinancialProfileResponseDTO created = financialProfileService.createProfile(
                userDetails.getUsername(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /tenant/financial-profile
     * Returns the raw saved profile data (not computed affordability figures).
     */
    @GetMapping
    public ResponseEntity<TenantFinancialProfileResponseDTO> getProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(financialProfileService.getProfile(userDetails.getUsername()));
    }

    /**
     * PUT /tenant/financial-profile
     * Fully replaces the authenticated user's financial profile data.
     */
    @PutMapping
    public ResponseEntity<TenantFinancialProfileResponseDTO> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TenantFinancialProfileRequestDTO request
    ) {
        return ResponseEntity.ok(
                financialProfileService.updateProfile(userDetails.getUsername(), request)
        );
    }

    /**
     * GET /tenant/financial-profile/compute
     * Calls FastAPI's /financial/profile live with the saved profile data
     * and returns the computed affordability figures (max_sustainable_rent,
     * financial_health, emergency_fund_status, etc.). Never cached — always
     * fresh, since the underlying calculation logic can evolve independently
     * of the stored input data.
     */
    @GetMapping("/compute")
    public ResponseEntity<FinancialProfileMlResponseDTO> computeProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(financialProfileService.computeProfile(userDetails.getUsername()));
    }

    /**
     * DELETE /tenant/financial-profile
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        financialProfileService.deleteProfile(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}