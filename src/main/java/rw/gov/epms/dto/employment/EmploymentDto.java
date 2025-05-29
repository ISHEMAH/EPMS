package rw.gov.epms.dto.employment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.gov.epms.dto.employee.EmployeeDto;
import rw.gov.epms.model.Employment.EmploymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentDto {
    private Long id;
    private String code;
    private EmployeeDto employee;
    private String department;
    private String position;
    private BigDecimal baseSalary;
    private EmploymentStatus status;
    private LocalDate joiningDate;
}