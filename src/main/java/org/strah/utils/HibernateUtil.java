package org.strah.utils;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.strah.model.applications.Application;
import org.strah.model.claims.Claim;
import org.strah.model.policies.*;
import org.strah.model.types.InsuranceType;
import org.strah.model.types.RiskCoeff;
import org.strah.model.types.TermCoeff;
import org.strah.model.users.AppUser;

public class HibernateUtil {

    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration cfg = new Configuration().configure()

                    /* ---- явно регистрируем все @Entity ---- */
                    .addAnnotatedClass(AppUser.class)
                    .addAnnotatedClass(Claim.class)
                    .addAnnotatedClass(Application.class)
                    .addAnnotatedClass(InsuranceType.class)
                    .addAnnotatedClass(InsurancePolicy.class)
                    .addAnnotatedClass(StandardPolicy.class)
                    .addAnnotatedClass(FinancialRiskPolicy.class)
                    .addAnnotatedClass(org.strah.model.types.RiskCoeff.class)
                    .addAnnotatedClass(org.strah.model.types.ApplicationTemplate.class)
                    .addAnnotatedClass(org.strah.model.types.RiskCoeffHistory.class)
                    .addAnnotatedClass(org.strah.model.applications.Application.class)
                    .addAnnotatedClass(org.strah.model.applications.ApplicationAnswer.class)
                    .addAnnotatedClass(RiskCoeff.class)
                    .addAnnotatedClass(TermCoeff.class);

            return cfg.buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() { return sessionFactory; }

    public static void shutdown() { getSessionFactory().close(); }
}
