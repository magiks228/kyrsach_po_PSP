package org.strah.utils;

import org.hibernate.Session;
import org.strah.model.applications.Application;
import org.strah.model.applications.ApplicationAnswer;
import org.strah.model.types.*;

import java.util.*;
import java.util.stream.Collectors;

public class PremiumCalculator {

    public double calculate(Application app, Session s) {
        // 1) Тип
        InsuranceType it = s.createQuery(
                        "from InsuranceType where code = :c", InsuranceType.class)
                .setParameter("c", app.getTypeCode())
                .uniqueResult();

        double base = (it.getBaseRateMin() + it.getBaseRateMax()) / 2.0;

        // 2) Коэффициенты опций
        double kOpt = app.getAnswers().stream()
                .mapToDouble(ans -> {
                    // безопасный запрос (чтобы не падало при отсутствующем rc)
                    return s.createQuery(
                                    "from RiskCoeff rc " +
                                            " where rc.typeCode    = :tc" +
                                            "   and rc.coeffGroup = :g" +
                                            "   and rc.optionCode = :oc",
                                    RiskCoeff.class)
                            .setParameter("tc", app.getTypeCode())
                            .setParameter("g", ans.getCoeffGroup())
                            .setParameter("oc", ans.getOptionCode())
                            .getResultList()
                            .stream()
                            .findFirst()
                            .map(RiskCoeff::getValue)
                            .orElse(1.0);
                })
                .reduce(1.0, (a, b) -> a * b);

        // 3) Франшиза
        double kFr = 1.0 - it.getFranchisePercent();

        // 4) Срок
        int m = app.getTermMonths();
        double kTerm = s.createQuery("from TermCoeff", TermCoeff.class)
                .getResultList().stream()
                .filter(t -> t.hit(m))
                .mapToDouble(TermCoeff::getCoeff)
                .findFirst()
                .orElse(1.0);

        return app.getCoverageAmount() * base * kOpt * kTerm * kFr;
    }
}