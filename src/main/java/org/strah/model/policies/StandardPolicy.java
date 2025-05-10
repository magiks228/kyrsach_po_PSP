package org.strah.model.policies;

import org.strah.model.types.InsuranceType;
import org.strah.model.users.AppUser;

import java.time.LocalDate;

import jakarta.persistence.Entity;

@Entity
public class StandardPolicy extends InsurancePolicy {

    public StandardPolicy() {}        // Hibernate

    public StandardPolicy(String num, LocalDate start, LocalDate end,
                          double premium, AppUser client,
                          InsuranceType type) {
        super(num, start, end, premium, client, type);
    }

    @Override
    public String getPolicyType() { return type.getName(); }
}
