package org.strah.utils;

import org.hibernate.Session;
import org.strah.model.applications.Application;
import org.strah.model.applications.ApplicationAnswer;
import org.strah.model.types.*;

import java.util.*;
import java.util.stream.Collectors;

public class PremiumCalculator {

    public double calculate(Application app, Session s) {
        InsuranceType it = s.get(InsuranceType.class,
                s.createQuery(
                                "select it.id from InsuranceType it where it.code = :code", Long.class)
                        .setParameter("code", app.getTypeCode())
                        .getSingleResult()
        );
        double base = it.getBaseRateMin();
        double kOptions = app.getAnswers().stream()
                .mapToDouble(ans -> {
                    RiskCoeff rc = s.createQuery(
                                    "from RiskCoeff where typeCode = :tc and group = :g and optionCode = :oc", RiskCoeff.class)
                            .setParameter("tc", app.getTypeCode())
                            .setParameter("g", ans.getCoeffGroup())
                            .setParameter("oc", ans.getOptionCode())
                            .getSingleResult();
                    return rc.getValue();
                }).reduce(1.0, (a,b)->a*b);

        double kFranchise = 1 - it.getFranchisePercent();

        int months = app.getTermMonths();
        double kTerm = s.createQuery("from TermCoeff", TermCoeff.class)
                .getResultList().stream()
                .filter(t -> t.hit(months))
                .mapToDouble(TermCoeff::getKValue)
                .findFirst().orElse(1.0);

        return app.getCoverageAmount() * base * kOptions * kTerm * kFranchise;
    }
}
