package org.strah.model.policies;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_schedule",
        indexes = @Index(name = "idx_ps_policy_id", columnList = "policy_id"))
public class PaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ps_seq")
    @SequenceGenerator(
            name = "ps_seq",
            sequenceName = "payment_schedule_id_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private InsurancePolicy policy;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "is_paid", nullable = false)
    private boolean paid = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PaymentSchedule() { }

    public PaymentSchedule(InsurancePolicy policy, LocalDate dueDate, double amount) {
        this.policy    = policy;
        this.dueDate   = dueDate;
        this.amount    = amount;
        this.paid      = false;
        this.createdAt = LocalDateTime.now();
    }

    // === геттеры и сеттеры ===

    public Long getId() { return id; }

    public InsurancePolicy getPolicy() { return policy; }
    public void setPolicy(InsurancePolicy policy) { this.policy = policy; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
