package org.strah.model.types;

import jakarta.persistence.*;
import org.strah.model.users.AppUser;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_coeff_history")
public class RiskCoeffHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private RiskCoeff coeff;

    @Column(name = "old_value", nullable = false)
    private double oldValue;

    @Column(name = "new_value", nullable = false)
    private double newValue;

    @Column(nullable = false)
    private LocalDateTime ts = LocalDateTime.now();

    @ManyToOne(optional = false)
    private AppUser changedBy;

    /* конструктор для Hibernate */ protected RiskCoeffHistory() {}

    public RiskCoeffHistory(RiskCoeff c, double oldV, double newV, AppUser by){
        this.coeff = c; this.oldValue = oldV; this.newValue = newV; this.changedBy = by;
    }
}
