package org.strah.model.applications;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "application_answers")
@IdClass(ApplicationAnswer.PK.class)
public class ApplicationAnswer {

    @Id
    @Column(name = "app_id")
    private Long appId;

    @Id
    @Column(name = "coeff_group", length = 50)
    private String coeffGroup;

    @Id
    @Column(name = "option_code", length = 50)
    private String optionCode;

    /** JPA-конструктор */
    protected ApplicationAnswer() { }

    /** Удобный конструктор */
    public ApplicationAnswer(Application app, String coeffGroup, String optionCode) {
        this.appId      = app.getId();
        this.coeffGroup = coeffGroup;
        this.optionCode = optionCode;
    }

    // геттеры
    public Long getAppId() { return appId; }
    public String getCoeffGroup() { return coeffGroup; }
    public String getOptionCode() { return optionCode; }

    // вложенный класс для composite key
    public static class PK implements Serializable {
        private Long appId;
        private String coeffGroup;
        private String optionCode;

        public PK() {}
        public PK(Long appId, String coeffGroup, String optionCode) {
            this.appId = appId;
            this.coeffGroup = coeffGroup;
            this.optionCode = optionCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK)) return false;
            PK pk = (PK)o;
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
