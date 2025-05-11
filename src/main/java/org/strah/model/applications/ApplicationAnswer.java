package org.strah.model.applications;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * Ответы на заявку: связь ManyToOne с Application + composite PK (appId, coeffGroup, optionCode)
 */
@Entity
@Table(name = "application_answers")
@IdClass(ApplicationAnswer.PK.class)
public class ApplicationAnswer {

    // составной ключ: ссылка на заявку
    @Id
    @Column(name = "app_id", nullable = false)
    private Long appId;

    // связь с родительской сущностью заявки
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_id", insertable = false, updatable = false)
    private Application application;

    // группа коэффициентов
    @Id
    @Column(name = "coeff_group", length = 50, nullable = false)
    private String coeffGroup;

    // выбранный опционный код
    @Id
    @Column(name = "option_code", length = 50, nullable = false)
    private String optionCode;

    /** JPA-конструктор (Hibernate) */
    protected ApplicationAnswer() { }

    /**
     * Удобный конструктор для создания нового ответа.
     * При этом поле appId будет дублироваться в join-столбце.
     */
    public ApplicationAnswer(Application app, String coeffGroup, String optionCode) {
        this.application = app;
        this.appId       = app.getId();
        this.coeffGroup  = coeffGroup;
        this.optionCode  = optionCode;
    }

    // геттеры (сеттеры не нужны, если вы не планируете менять после создания)
    public Long getAppId() { return appId; }
    public Application getApplication() { return application; }
    public String getCoeffGroup() { return coeffGroup; }
    public String getOptionCode() { return optionCode; }


    /** Вложенный класс для composite PK */
    public static class PK implements Serializable {
        private Long   appId;
        private String coeffGroup;
        private String optionCode;

        public PK() { }

        public PK(Long appId, String coeffGroup, String optionCode) {
            this.appId      = appId;
            this.coeffGroup = coeffGroup;
            this.optionCode = optionCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK)) return false;
            PK pk = (PK) o;
            return Objects.equals(appId, pk.appId)
                    && Objects.equals(coeffGroup, pk.coeffGroup)
                    && Objects.equals(optionCode, pk.optionCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(appId, coeffGroup, optionCode);
        }
    }
}
