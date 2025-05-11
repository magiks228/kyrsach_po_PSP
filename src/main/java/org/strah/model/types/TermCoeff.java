package org.strah.model.types;

import jakarta.persistence.*;

@Entity
@Table(name = "k_term")
public class TermCoeff {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="months_from") private int monthsFrom;
    @Column(name="months_to")   private int monthsTo;
    @Column(name="k_value")     private double kValue;

    public TermCoeff() {}         // JPA

    public boolean hit(int m){ return m>=monthsFrom && m<=monthsTo; }
    public double  getKValue(){ return kValue; }
}
