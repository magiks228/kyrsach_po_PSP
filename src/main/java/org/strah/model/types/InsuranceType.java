package org.strah.model.types;

import jakarta.persistence.*;

/**
 * Cправочник «Виды страхования».
 * Хранит базовые ставки и параметры, которые понадобятся
 * калькулятору премии и фильтрам в GUI.
 */
@Entity
@Table(name = "insurance_types")
public class InsuranceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** короткий код (PROPERTY, BI, GL …) */
    @Column(length = 20, unique = true, nullable = false)
    private String code;

    /** русское наименование для вывода в интерфейсе */
    @Column(name = "name_ru", length = 120, nullable = false)
    private String name;

    /** базовая ставка, минимальная / максимальная */
    @Column(name = "base_rate_min") private double baseRateMin;
    @Column(name = "base_rate_max") private double baseRateMax;

    /** рекомендуемые лимиты ответственности (можно не использовать сейчас) */
    @Column(name = "limit_min")     private double limitMin;
    @Column(name = "limit_max")     private double limitMax;

    /** типичный срок (месяцы) —‑ для шаблонов заявок */
    @Column(name = "default_term")  private int defaultTerm;

    /* ---------- конструкторы ---------- */
    public InsuranceType() {}           // Hibernate
    public InsuranceType(String code, String name,
                         double baseMin, double baseMax,
                         double limMin, double limMax, int term){
        this.code = code; this.name = name;
        this.baseRateMin  = baseMin;  this.baseRateMax = baseMax;
        this.limitMin     = limMin;   this.limitMax    = limMax;
        this.defaultTerm  = term;
    }

    /* ---------- геттеры: требуются сервисам/GUI ---------- */
    public Long   getId()          { return id; }
    public String getCode()        { return code; }
    public String getName()        { return name; }
    public double getBaseRateMin() { return baseRateMin; }
    public double getBaseRateMax() { return baseRateMax; }
    public double getLimitMin()    { return limitMin; }
    public double getLimitMax()    { return limitMax; }
    public int    getDefaultTerm() { return defaultTerm; }

    @Override public String toString(){ return name; }
}
