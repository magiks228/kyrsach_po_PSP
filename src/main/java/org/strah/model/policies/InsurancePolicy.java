package org.strah.model.policies;

import jakarta.persistence.*;
import org.strah.model.applications.Application;
import org.strah.model.types.InsuranceType;
import org.strah.model.users.AppUser;
import org.strah.model.claims.Claim;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "insurance_policies")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class InsurancePolicy implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "policy_number", unique = true, nullable = false)
    protected String policyNumber;

    @Column(name = "start_date", nullable = false)
    protected LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    protected LocalDate endDate;

    @Column(nullable = false)
    protected double premium;

    /* ---------- FK‑связи ---------- */

    /** FK **customer_id** → users(id)  (ВЕРНУЛИ старое имя колонки) */
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    protected AppUser customer;

    /** FK type_id → insurance_types(id) */
    @ManyToOne(optional = false)
    @JoinColumn(name = "type_id")
    protected InsuranceType type;

    /** заявка‑основание полиса */
    @ManyToOne
    @JoinColumn(name = "application_id")
    protected Application application;

    /* ---------- конструкторы ---------- */
    public InsurancePolicy() {}
    public InsurancePolicy(String number, LocalDate start, LocalDate end,
                           double premium, AppUser customer, InsuranceType type){
        this.policyNumber = number;
        this.startDate    = start;
        this.endDate      = end;
        this.premium      = premium;
        this.customer     = customer;
        this.type         = type;
    }

    /* ---------- геттеры ---------- */
    public Long getId()                { return id; }
    public String getPolicyNumber()    { return policyNumber; }
    public LocalDate getStartDate()    { return startDate; }
    public LocalDate getEndDate()      { return endDate; }
    public double getPremium()         { return premium; }
    public AppUser getCustomer()       { return customer; }
    public InsuranceType getType()     { return type; }
    public void setApplication(Application a){ this.application = a; }

    /* ---------- претензии ---------- */
    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Claim> claims = new ArrayList<>();
    public List<Claim> getClaims(){ return claims; }

    /* ---------- util‑методы ---------- */
    public String toListRow(){
        return policyNumber + " " + type.getCode() + " " + premium;
    }
    public String getPolicyType(){ return type.getName(); }

    @Override
    public String toString(){
        return "Полис[" + policyNumber + "] " + getPolicyType() +
                " клиента " + customer.getLogin() +
                String.format(" (%.2f BYN)", premium);
    }

    @Override public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof InsurancePolicy that)) return false;
        return Objects.equals(id, that.id);
    }
    @Override public int hashCode(){ return Objects.hash(id); }
}
