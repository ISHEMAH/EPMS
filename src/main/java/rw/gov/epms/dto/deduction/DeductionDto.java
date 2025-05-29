package rw.gov.epms.dto.deduction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductionDto {
    private Long id;
    private String code;
    private String deductionName;
    private BigDecimal percentage;
    private boolean isAddition;
}