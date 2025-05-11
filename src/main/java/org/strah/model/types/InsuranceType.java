package org.strah.model.types;

import jakarta.persistence.*;

@Entity
@Table(name = "insurance_types",
        uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class InsuranceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="code", length = 20, nullable = false)
    private String code;

    @Column(name="name_ru", length = 120, nullable = false)
    private String nameRu;

    @Column(name="base_rate_min", nullable = false)
    private double baseRateMin;

    @Column(name="base_rate_max", nullable = false)
    private double baseRateMax;

    @Column(name="limit_min", nullable = false)
    private double limitMin;

    @Column(name="limit_max", nullable = false)
    private double limitMax;

    @Column(name="default_term", nullable = false)
    private int defaultTerm;

    @Column(name="franchise_pct", nullable = false)
    private double franchisePercent;

    /** JPA-конструктор */
    protected InsuranceType() { }

    /** Удобный конструктор */
    public InsuranceType(String code,
                         String nameRu,
                         double limitMin,
                         double limitMax,
                         double baseRateMin,
                         double baseRateMax,
                         int defaultTerm,
                         double franchisePercent) {
        this.code              = code;
        this.nameRu            = nameRu;
        this.limitMin          = limitMin;
        this.limitMax          = limitMax;
        this.baseRateMin       = baseRateMin;
        this.baseRateMax       = baseRateMax;
        this.defaultTerm       = defaultTerm;
        this.franchisePercent  = franchisePercent;
    }

    // геттеры
    public Long   getId()           { return id; }
    public String getCode()         { return code; }
    public String getNameRu()       { return nameRu; }
    public double getBaseRateMin()  { return baseRateMin; }
    public double getBaseRateMax()  { return baseRateMax; }
    public double getLimitMin()     { return limitMin; }
    public double getLimitMax()     { return limitMax; }
    public int    getDefaultTerm()  { return defaultTerm; }
    public double getFranchisePercent() { return franchisePercent; }

    public String getName() {
        return nameRu;
    }
}
