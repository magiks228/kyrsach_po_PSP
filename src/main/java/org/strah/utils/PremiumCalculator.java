package org.strah.utils;

import org.hibernate.Session;
import org.strah.model.applications.Application;
import org.strah.model.types.RiskCoeff;
import org.strah.utils.HibernateUtil;

import java.util.List;
import java.util.Map;

public class PremiumCalculator {

    /**
     * @param app        заявка
     * @param overrides  ручные изменения: код‑>значение (может быть пустым)
     */
    public double calculate(Application app, Map<String, Double> overrides){
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            List<RiskCoeff> coeffs = s.createQuery(
                            "from RiskCoeff c where c.type.id = :tid", RiskCoeff.class)
                    .setParameter("tid", app.getType().getId())
                    .list();

            double kProduct = 1.0;
            for (RiskCoeff rc : coeffs) {
                double k = overrides.getOrDefault(rc.getCode(), rc.getValue());
                kProduct *= k;
            }
            double base = app.getType().getBaseRateMin();      // пока min‑ставка
            return base * kProduct * app.getMonths() / 12.0;
        }
    }
}
