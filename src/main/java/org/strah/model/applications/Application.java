package org.strah.model.applications;

import jakarta.persistence.*;
import java.util.List;
import java.util.ArrayList;


import org.strah.model.types.InsuranceType;
import org.strah.model.users.AppUser;

import java.time.LocalDate;

@Entity
@Table(name = "applications")
public class Application {

    /* -------------------- колонки -------------------- */

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    private AppUser client;

    @ManyToOne(optional = false)
    private InsuranceType insuranceType;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.NEW;

    @OneToMany(mappedBy = "application",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<ApplicationAnswer> answers = new ArrayList<>();

    @Column(name="coverage_amount") private double coverageAmount;
    public double getCoverageAmount(){ return coverageAmount; }

    /* удобный helper */
    public void addAnswer(String field, String value){
        ApplicationAnswer aa = new ApplicationAnswer(this, field, value);
        this.answers.add(aa);
    }

    /** заказанный срок страхования (мес.) */
    private int months;

    /** рассчитанная премия  */
    private double premium;

    private LocalDate createdAt = LocalDate.now();

    /* -------------------- конструкторы ---------------- */

    public Application() {}                 // для Hibernate

    public Application(AppUser cl,
                       InsuranceType t,
                       int months) {
        this.client         = cl;
        this.insuranceType  = t;
        this.months         = months;
    }

    /* -------------------- getters / setters ---------- */

    public Long              getId()      { return id; }
    public InsuranceType     getType()    { return insuranceType; }
    public ApplicationStatus getStatus()  { return status; }
    public AppUser           getClient()  { return client; }

    public int               getMonths()  { return months; }
    public double            getPremium() { return premium; }

    public void setStatus (ApplicationStatus s){ this.status  = s; }
    public void setPremium(double p)           { this.premium = p; }
}
