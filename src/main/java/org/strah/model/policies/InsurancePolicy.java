package org.strah.model.policies;

import jakarta.persistence.*;
import org.strah.model.types.InsuranceType;
import org.strah.model.users.AppUser;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "insurance_policies")
public class InsurancePolicy implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_number", nullable = false, unique = true)
    private String policyNumber;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "premium")
    private double premium;

    @Column(name = "coverage_amount")
    private Double coverageAmount;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private AppUser customer;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private InsuranceType type;

    @OneToOne
    @JoinColumn(name = "application_id")
    private org.strah.model.applications.Application application;

    public InsurancePolicy() {}

    public InsurancePolicy(String policyNumber,
                           LocalDate startDate,
                           LocalDate endDate,
                           double premium,
                           double coverageAmount,
                           AppUser customer,
                           InsuranceType type) {
        this.policyNumber = policyNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        this.premium = premium;
        this.coverageAmount = coverageAmount;
        this.customer = customer;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public double getPremium() {
        return premium;
    }

    public Double getCoverageAmount() {
        return coverageAmount;
    }

    public void setCoverageAmount(Double coverageAmount) {
        this.coverageAmount = coverageAmount;
    }

    public AppUser getCustomer() {
        return customer;
    }

    public InsuranceType getType() {
        return type;
    }

    public org.strah.model.applications.Application getApplication() {
        return application;
    }

    public void setApplication(org.strah.model.applications.Application application) {
        this.application = application;
    }
}
