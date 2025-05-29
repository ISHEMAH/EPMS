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
@Table(name = "deductions")
public class Deduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(name = "deduction_name", nullable = false, unique = true)
    private String deductionName;

    @Column(nullable = false)
    private BigDecimal percentage;

    @Column(nullable = false)
    private boolean isAddition;

    // Helper method to get percentage as a decimal (e.g., 30% -> 0.3)
    @Transient
    public BigDecimal getPercentageAsDecimal() {
        return percentage.divide(BigDecimal.valueOf(100));
    }
}