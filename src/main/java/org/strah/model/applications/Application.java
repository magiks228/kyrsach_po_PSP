    package org.strah.model.applications;

    import jakarta.persistence.*;
    import java.time.LocalDate;
    import java.util.List;

    @Entity
    @Table(name = "applications")
    public class Application {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_seq")
        @SequenceGenerator(
                name = "app_seq",
                sequenceName = "applications_id_seq",
                allocationSize = 1
        )
        @Column(name = "id")
        private Long id;

        /** Идентификатор клиента (user_id) */
        @Column(name = "customer_id", nullable = false)
        private Long customerId;

        /** Код вида страхования (ссылка на insurance_types.code) */
        @Column(name = "type_code", length = 20, nullable = false)
        private String typeCode;

        /** Сумма покрытия, которую запрашивает клиент (insured sum) */
        @Column(name = "coverage_amount")
        private Double coverageAmount;

        /** Срок в месяцах */
        @Column(name = "term_months", nullable = false)
        private Integer termMonths;

        /** Статус заявки (NEW, CALCULATED, PAID и т.д.) */
        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        private ApplicationStatus status;

        /** Рассчитанная премия */
        @Column(name = "premium")
        private Double premium;

        /** Дата начала действия (после оплаты) */
        @Column(name = "start_date")
        private LocalDate startDate;

        /** Дата окончания действия */
        @Column(name = "end_date")
        private LocalDate endDate;

        /** Список ответов по коэффициентам */
        @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<ApplicationAnswer> answers;

        /** JPA-конструктор */
        protected Application() { }

        // Конструктор для удобства (можно оставить по минимуму)
        public Application(Long customerId, String typeCode, Double coverageAmount, Integer termMonths) {
            this.customerId     = customerId;
            this.typeCode       = typeCode;
            this.coverageAmount = coverageAmount;
            this.termMonths     = termMonths;
            this.status         = ApplicationStatus.NEW;
            this.premium        = 0.0;
        }

        // ===================== Геттеры и сеттеры =====================

        public Long getId() {
            return id;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public String getTypeCode() {
            return typeCode;
        }

        public void setTypeCode(String typeCode) {
            this.typeCode = typeCode;
        }

        public Double getCoverageAmount() {
            return coverageAmount;
        }

        public void setCoverageAmount(Double coverageAmount) {
            this.coverageAmount = coverageAmount;
        }

        public Integer getTermMonths() {
            return termMonths;
        }

        public void setTermMonths(Integer termMonths) {
            this.termMonths = termMonths;
        }

        public ApplicationStatus getStatus() { return status; }
        public void setStatus(ApplicationStatus status) { this.status = status; }

        public Double getPremium() {
            return premium;
        }

        public void setPremium(Double premium) {
            this.premium = premium;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public List<ApplicationAnswer> getAnswers() {
            return answers;
        }

        public void setAnswers(List<ApplicationAnswer> answers) {
            this.answers = answers;
        }
    }
