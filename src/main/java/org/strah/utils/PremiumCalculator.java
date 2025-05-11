package org.strah.utils;

import org.hibernate.Session;
import org.strah.model.applications.Application;
import org.strah.model.applications.ApplicationAnswer;
import org.strah.model.types.*;

import java.util.*;
import java.util.stream.Collectors;

public class PremiumCalculator {

    private final Session s;
    public PremiumCalculator(Session s){ this.s = s; }

    public double calc(Application app){
        double coverage   = app.getCoverageAmount();   // поле добавлено в Application
        InsuranceType it  = app.getType();

        /* 1. базовая ставка */
        double base = (it.getBaseRateMin()+it.getBaseRateMax())/2.0;

        /* 2. коэффициенты по ответам клиента */
        Map<String,String> answers = s.createQuery(
                        "from ApplicationAnswer where application.id=:id",ApplicationAnswer.class)
                .setParameter("id", app.getId())
                .stream()
                .collect(Collectors.toMap(ApplicationAnswer::getCoeffGroup,
                        ApplicationAnswer::getOptionCode));

        double kOptions = 1.0;
        if(!answers.isEmpty()){
            List<RiskCoeff> list = s.createQuery(
                            "from RiskCoeff where typeCode=:c and group in (:g)", RiskCoeff.class)
                    .setParameter("c", it.getCode())
                    .setParameterList("g", answers.keySet())
                    .list();
            for(RiskCoeff rc: list)
                if(answers.get(rc.getGroup()).equals(rc.getOptionCode()))
                    kOptions *= rc.getValue();
        }

        /* 3. K_TERM */
        int m = app.getMonths();
        double kTerm = s.createQuery("from TermCoeff", TermCoeff.class)
                .stream().filter(t->t.hit(m))
                .map(TermCoeff::getKValue).findFirst().orElse(1.0);

        /* 4. франшиза */
        double kFranchise = 1 - it.getFranchisePercent();

        return coverage * base * kOptions * kTerm * kFranchise;
    }
}
