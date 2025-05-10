package org.strah.model.applications;

import jakarta.persistence.*;

@Entity
@Table(name = "application_answers")
public class ApplicationAnswer {

    @Id @GeneratedValue
    private Long id;

    /* FK → applications.id */
    @ManyToOne(optional = false)
    @JoinColumn(name = "application_id")
    private Application application;

    /* ====== изменения только здесь  ====== */
    /** ключ анкеты (колонка `field` в БД)  */
    @Column(name = "field", length = 100, nullable = false)
    private String fieldCode;

    /** введённое значение (колонка `value` в БД) */
    @Column(name = "value", length = 500, nullable = false)
    private String fieldValue;
    /* ====================================== */

    public ApplicationAnswer() {}             // Hibernate

    public ApplicationAnswer(Application app,
                             String field, String value) {
        this.application = app;
        this.fieldCode   = field;
        this.fieldValue  = value;
    }

    /* getters */
    public Long        getId()        { return id; }
    public String      getFieldCode() { return fieldCode; }
    public String      getFieldValue(){ return fieldValue; }
    public Application getApplication(){ return application; }
}
