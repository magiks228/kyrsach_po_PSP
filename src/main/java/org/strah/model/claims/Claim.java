package org.strah.model.claims;

import jakarta.persistence.*;
import org.strah.model.policies.InsurancePolicy;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "claims")
public class Claim implements Serializable {

    public enum Status { NEW, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne              // много заявок к одному полису
    @JoinColumn(name = "policy_id", nullable = false)
    private InsurancePolicy policy;

    @Column(name = "date_created", nullable = false)
    private LocalDate dateCreated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private double amount;

    @Column(length = 1000)
    private String description;

    /* ----------- конструкторы ----------- */
    public Claim() {}

    public Claim(InsurancePolicy policy,
                 LocalDate dateCreated,
                 double amount,
                 String description) {
        this.policy      = policy;
        this.dateCreated = dateCreated;
        this.amount      = amount;
        this.description = description;
        this.status      = Status.NEW;
    }

    /* ----------- геттеры/сеттеры -------- */
    public Long getId()                  { return id; }
    public InsurancePolicy getPolicy()   { return policy; }
    public LocalDate getDateCreated()    { return dateCreated; }
    public Status getStatus()            { return status; }
    public void setStatus(Status s)      { this.status = s; }
    public double getAmount()            { return amount; }
    public void setAmount(double a)      { this.amount = a; }
    public String getDescription()       { return description; }
    public void setDescription(String d) { this.description = d; }

    @Override
    public String toString() {
        return "Claim{" +
                "id=" + id +
                ", policy=" + policy.getPolicyNumber() +
                ", date=" + dateCreated +
                ", status=" + status +
                ", amount=" + amount +
                '}';
    }
}
