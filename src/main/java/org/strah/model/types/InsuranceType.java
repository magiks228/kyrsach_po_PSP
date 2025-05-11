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


    @Column(name = "franchise_pct")      // 0.0‑1.0  (5%  →  0.05)
    private double franchisePercent;

    // ↓ замените оба конструктора
    public InsuranceType() {}     // JPA

    public InsuranceType(String code, String nameRu,
                         double baseMin, double baseMax,
                         double limMin, double limMax,
                         int defTerm, double franchisePercent){
        this.code = code;   this.name   = nameRu;
        this.baseRateMin = baseMin;     this.baseRateMax = baseMax;
        this.limitMin    = limMin;      this.limitMax    = limMax;
        this.defaultTerm = defTerm;
        this.franchisePercent = franchisePercent;
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
