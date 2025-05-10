package org.strah.model.types;

import jakarta.persistence.*;

@Entity
@Table(name = "application_templates")
public class ApplicationTemplate {

    @Id @GeneratedValue
    private Long id;

    @OneToOne(optional = false)
    private InsuranceType type;

    /** JSON-описание полей (frontend динамически строит форму) */
    @Column(columnDefinition = "json")
    private String jsonSpec;

    /* getters / setters */
    public InsuranceType getType() { return type; }
    public String getJsonSpec()    { return jsonSpec; }
    public void setType    (InsuranceType t){ this.type = t; }
    public void setJsonSpec(String j)      { this.jsonSpec = j; }
}
