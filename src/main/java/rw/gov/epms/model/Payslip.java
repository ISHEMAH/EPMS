package rw.gov.epms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payslips", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"employee_id", "month", "year"})
})
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "house_amount", nullable = false)
    private BigDecimal houseAmount;

    @Column(name = "transport_amount", nullable = false)
    private BigDecimal transportAmount;

    @Column(name = "employee_taxed_amount", nullable = false)
    private BigDecimal employeeTaxedAmount;

    @Column(name = "pension_amount", nullable = false)
    private BigDecimal pensionAmount;

    @Column(name = "medical_insurance_amount", nullable = false)
    private BigDecimal medicalInsuranceAmount;

    @Column(name = "other_taxed_amount", nullable = false)
    private BigDecimal otherTaxedAmount;

    @Column(name = "gross_salary", nullable = false)
    private BigDecimal grossSalary;

    @Column(name = "net_salary", nullable = false)
    private BigDecimal netSalary;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayslipStatus status;

    @Column(name = "base_salary", nullable = false)
    private BigDecimal baseSalary;

    public enum PayslipStatus {
        PENDING, PAID
    }
}