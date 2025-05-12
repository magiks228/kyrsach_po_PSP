package org.strah.model.policies;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.strah.model.users.AppUser;
import org.strah.model.types.InsuranceType;

@Entity
@Table(name = "standard_policies")
@PrimaryKeyJoinColumn(name = "policy_id")
public class StandardPolicy extends InsurancePolicy {

    protected StandardPolicy() {
        super();
    }

    /**
     * Конструктор для обычных (без покрытия) полисов.
     */
    public StandardPolicy(String number,
                          LocalDate start,
                          LocalDate end,
                          double premium,
                          AppUser customer,
                          InsuranceType type) {
        super(number, start, end, premium, customer, type);
    }
}
