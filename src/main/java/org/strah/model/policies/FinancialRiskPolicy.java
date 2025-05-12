package org.strah.model.policies;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.strah.model.users.AppUser;
import org.strah.model.types.InsuranceType;

@Entity
@Table(name = "financial_risk_policies")
@PrimaryKeyJoinColumn(name = "policy_id")
public class FinancialRiskPolicy extends InsurancePolicy {

    @Column(name = "coverage_amount", nullable = false)
    private double coverageAmount;

    protected FinancialRiskPolicy() {
        super();
    }

    /**
     * Конструктор для полисов с указанием покрытия.
     */
    public FinancialRiskPolicy(String number,
                               LocalDate start,
                               LocalDate end,
                               double premium,
                               double coverageAmount,
                               AppUser customer,
                               InsuranceType type) {
        super(number, start, end, premium, customer, type);
        this.coverageAmount = coverageAmount;
    }

    public double getCoverageAmount() {
        return coverageAmount;
    }
}
