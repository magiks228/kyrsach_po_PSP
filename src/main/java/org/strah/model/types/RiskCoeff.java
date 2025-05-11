package org.strah.model.types;

import jakarta.persistence.*;

@Entity
@Table(name = "risk_coefficients",
        uniqueConstraints = @UniqueConstraint(columnNames = {"type_code","coeff_group","option_code"}))
public class RiskCoeff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_code", length = 20, nullable = false)
    private String typeCode;

    @Column(name = "coeff_group", length = 50, nullable = false)
    private String group;

    @Column(name = "option_code", length = 50, nullable = false)
    private String optionCode;

    @Column(name = "option_name", length = 100, nullable = false)
    private String optionName;

    @Column(name = "value", nullable = false)
    private double value;

    /** JPA-конструктор */
    protected RiskCoeff() { }

    /** Удобный конструктор */
    public RiskCoeff(String typeCode,
                     String group,
                     String optionCode,
                     String optionName,
                     double value) {
        this.typeCode   = typeCode;
        this.group      = group;
        this.optionCode = optionCode;
        this.optionName = optionName;
        this.value      = value;
    }

    // геттеры
    public Long   getId()         { return id; }
    public String getTypeCode()   { return typeCode; }
    public String getGroup()      { return group; }
    public String getOptionCode(){ return optionCode; }
    public String getOptionName(){ return optionName; }
    public double getValue()      { return value; }
}
