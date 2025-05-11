package org.strah.model.applications;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "application_answers")
@IdClass(ApplicationAnswer.PK.class)
public class ApplicationAnswer {

    public ApplicationAnswer() {} // JPA needs it

    @Id @Column(name="app_id")       private Long appId;
    @Id @Column(name="coeff_group")  private String coeffGroup;
    @Column(name="option_code")      private String optionCode;

    /* -------- composite PK -------- */
    @Embeddable
    public static class PK implements Serializable{
        private Long   appId;
        private String coeffGroup;
        /* equals & hashCode обязательно для PK */
        @Override public boolean equals(Object o){
            if(this==o) return true;
            if(!(o instanceof PK p)) return false;
            return Objects.equals(appId,p.appId)&&
                    Objects.equals(coeffGroup,p.coeffGroup);
        }
        @Override public int hashCode(){ return Objects.hash(appId,coeffGroup); }
    }

    /* getters */
    public String getCoeffGroup() { return coeffGroup; }
    public String getOptionCode() { return optionCode; }
}
