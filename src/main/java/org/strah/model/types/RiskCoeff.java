package org.strah.model.types;

import jakarta.persistence.*;

@Entity
@Table(name = "risk_coefficients",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "type_code","coeff_group","option_code"}))
public class RiskCoeff {

    // + пустой конструктор
    public RiskCoeff() {}   // JPA


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_code",     nullable = false) private String typeCode;
    @Column(name = "coeff_group",   nullable = false) private String group;
    @Column(name = "option_code",   nullable = false) private String optionCode;
    @Column(name = "option_name",   nullable = false) private String optionName;

    @Column(name = "k_value",       nullable = false) private double value;

    /* -------- getters ---------- */
    public String getTypeCode()   { return typeCode; }
    public String getGroup()      { return group; }
    public String getOptionCode() { return optionCode; }
    public double getValue()      { return value; }

    public String getOptionName(){ return optionName; }
    public String getCoeffGroup(){ return group; }

}
