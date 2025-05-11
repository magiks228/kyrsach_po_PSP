package org.strah.model.types;

import jakarta.persistence.*;

@Entity
@Table(name = "term_coeffs")
public class TermCoeff {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "term_coeff_seq")
    @SequenceGenerator(
            name = "term_coeff_seq",
            sequenceName = "term_coeffs_id_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "month_from", nullable = false)
    private int monthFrom;

    @Column(name = "month_to", nullable = false)
    private int monthTo;

    @Column(name = "coeff", nullable = false)
    private double coeff;

    protected TermCoeff() { }

    public TermCoeff(int monthFrom, int monthTo, double coeff) {
        this.monthFrom = monthFrom;
        this.monthTo   = monthTo;
        this.coeff     = coeff;
    }

    public Long getId() { return id; }
    public int getMonthFrom() { return monthFrom; }
    public int getMonthTo() { return monthTo; }
    public double getCoeff() { return coeff; }

    /** Для поиска подходящего коэффициента */
    public boolean hit(int months) {
        return months >= monthFrom && months <= monthTo;
    }
}
