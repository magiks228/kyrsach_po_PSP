package org.strah.model.types;

import jakarta.persistence.*;

@Entity
@Table(name = "risk_coeffs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"type_id","code"}))
public class RiskCoeff {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private InsuranceType type;

    /** Код коэффициента: K_region, K_security … */
    @Column(nullable = false, length = 64)
    private String code;

    private double value;

    /* === getters === */
    public InsuranceType getType() { return type; }
    public String getCode()        { return code; }
    public double getValue()       { return value; }

    /* === setters === */
    public void setType (InsuranceType t){ this.type = t; }
    public void setCode (String c)      { this.code = c; }
    public void setValue(double v)      { this.value = v; }
}
