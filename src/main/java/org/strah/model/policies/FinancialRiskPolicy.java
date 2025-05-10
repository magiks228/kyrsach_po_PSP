package org.strah.model.policies;

import org.strah.model.types.InsuranceType;
import org.strah.model.users.AppUser;

import java.time.LocalDate;

import jakarta.persistence.Entity;

/**
 * Полис страхования финансовых рисков.
 * Отличается только названием (формула расчёта – та же, что в StandardPolicy).
 */

@Entity
public class FinancialRiskPolicy extends InsurancePolicy {

    public FinancialRiskPolicy() {}   // конструктор по умолчанию для Hibernate

    /* ---------- конструктор ---------- */
    public FinancialRiskPolicy(String num,
                               LocalDate start,
                               LocalDate end,
                               double premium,
                               AppUser client,
                               InsuranceType type) {
        super(num, start, end, premium, client, type);
    }

    /* ---------- обязательное переопределение ---------- */
    @Override
    public String getPolicyType() {
        return type.getName();          // «Финансовые риски» на русском
    }
}
