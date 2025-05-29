package rw.gov.epms.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.gov.epms.dto.employee.EmployeeDto;
import rw.gov.epms.dto.payroll.PayrollGenerationRequest;
import rw.gov.epms.dto.payroll.PayslipDto;
import rw.gov.epms.model.Employee;
import rw.gov.epms.model.Payslip;
import rw.gov.epms.repository.PayslipRepository;
import rw.gov.epms.service.PayrollService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final PayslipRepository payslipRepository;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> generatePayroll(@Valid @RequestBody PayrollGenerationRequest request) {
        try {
            List<Payslip> generatedPayslips = payrollService.generatePayroll(request.getMonth(), request.getYear());
            List<PayslipDto> payslipDtos = generatedPayslips.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(payslipDtos);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approvePayroll(@Valid @RequestBody PayrollGenerationRequest request) {
        try {
            List<Payslip> approvedPayslips = payrollService.approvePayroll(request.getMonth(), request.getYear());
            List<PayslipDto> payslipDtos = approvedPayslips.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(payslipDtos);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/month/{month}/year/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<PayslipDto>> getPayslipsByMonthAndYear(
            @PathVariable Integer month, @PathVariable Integer year) {
        List<PayslipDto> payslips = payslipRepository.findByMonthAndYear(month, year).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payslips);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<PayslipDto>> getAllPayslips() {
        List<PayslipDto> payslips = payslipRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payslips);
    }

    private PayslipDto convertToDto(Payslip payslip) {
        return PayslipDto.builder()
                .id(payslip.getId())
                .employee(convertEmployeeToDto(payslip.getEmployee()))
                .baseSalary(payslip.getBaseSalary())
                .houseAmount(payslip.getHouseAmount())
                .transportAmount(payslip.getTransportAmount())
                .employeeTaxedAmount(payslip.getEmployeeTaxedAmount())
                .pensionAmount(payslip.getPensionAmount())
                .medicalInsuranceAmount(payslip.getMedicalInsuranceAmount())
                .otherTaxedAmount(payslip.getOtherTaxedAmount())
                .grossSalary(payslip.getGrossSalary())
                .netSalary(payslip.getNetSalary())
                .month(payslip.getMonth())
                .year(payslip.getYear())
                .status(payslip.getStatus())
                .build();
    }

    private EmployeeDto convertEmployeeToDto(Employee employee) {
        return EmployeeDto.builder()
                .id(employee.getId())
                .code(employee.getCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .mobile(employee.getMobile())
                .dateOfBirth(employee.getDateOfBirth())
                .status(employee.getStatus())
                .roles(employee.getRoles())
                .build();
    }
}
