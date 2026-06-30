package olistay.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embeddable value object matching AvailableFundsBreakdown in
 * financial/profiler.py exactly. When present on TenantFinancialProfile,
 * this REPLACES the aggregate currentSavings figure entirely — same
 * override semantics as the Pydantic model.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableFundsBreakdown {

    @Column(name = "funds_checking_account")
    @Builder.Default
    private Double checkingAccount = 0.0;

    @Column(name = "funds_savings_account")
    @Builder.Default
    private Double savingsAccount = 0.0;

    @Column(name = "funds_cash_on_hand")
    @Builder.Default
    private Double cashOnHand = 0.0;

    /** Orange Money / MTN MoMo balance. */
    @Column(name = "funds_mobile_money")
    @Builder.Default
    private Double mobileMoney = 0.0;

    @Column(name = "funds_other")
    @Builder.Default
    private Double other = 0.0;

    /**
     * Mirrors AvailableFundsBreakdown.total() in profiler.py.
     */
    public double total() {
        return checkingAccount + savingsAccount + cashOnHand + mobileMoney + other;
    }
}
