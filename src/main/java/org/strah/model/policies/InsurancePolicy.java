package org.strah.model.policies;

import jakarta.persistence.*;
import org.strah.model.applications.Application;
import org.strah.model.claims.Claim;
import org.strah.model.types.InsuranceType;
import org.strah.model.users.AppUser;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "insurance_policies")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class InsurancePolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_number", unique = true, nullable = false)
    protected String policyNumber;

    @Column(name = "start_date", nullable = false)
    protected LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    protected LocalDate endDate;

    @Column(nullable = false)
    protected double premium;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    protected AppUser customer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    protected InsuranceType type;

    @ManyToOne
    @JoinColumn(name = "application_id")
    protected Application application;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Claim> claims = new ArrayList<>();

    protected InsurancePolicy() { }

    public InsurancePolicy(String number,
                           LocalDate start,
                           LocalDate end,
                           double premium,
                           AppUser customer,
                           InsuranceType type) {
        this.policyNumber = number;
        this.startDate    = start;
        this.endDate      = end;
        this.premium      = premium;
        this.customer     = customer;
        this.type         = type;
    }

    public Long getId() { return id; }
    public String getPolicyNumber() { return policyNumber; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public double getPremium() { return premium; }
    public AppUser getCustomer() { return customer; }
    public InsuranceType getType() { return type; }
    public Application getApplication() { return application; }
    public void setApplication(Application application) { this.application = application; }
    public List<Claim> getClaims() { return claims; }

    /** Для вывода в таблицу: номер, код типа и премия */
    public String toListRow() {
        return policyNumber + " " + type.getCode() + " " + premium;
    }

    /** Человеко-читаемое название типа */
    public String getPolicyType() {
        return type.getName();
    }

    @Override
    public String toString() {
        return "Полис[" + policyNumber + "] " + getPolicyType() +
                " клиента " + customer.getLogin() +
                String.format(" (%.2f BYN)", premium);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InsurancePolicy)) return false;
        InsurancePolicy that = (InsurancePolicy) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
