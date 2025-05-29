package rw.gov.epms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import rw.gov.epms.dto.employee.EmployeeDto;
import rw.gov.epms.dto.payroll.PayslipDto;
import rw.gov.epms.model.Employee;
import rw.gov.epms.model.Payslip;
import rw.gov.epms.repository.EmployeeRepository;
import rw.gov.epms.repository.PayslipRepository;

import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payslips")
@RequiredArgsConstructor
@Tag(name = "Payslip Management", description = "APIs for managing payslips")
@SecurityRequirement(name = "bearerAuth")
public class PayslipController {

    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;

    @Operation(summary = "Get all payslips", description = "Returns a list of all payslips. Accessible by ADMIN and MANAGER roles.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of payslips"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or MANAGER role")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<PayslipDto>> getAllPayslips() {
        List<PayslipDto> payslips = payslipRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payslips);
    }

    @Operation(summary = "Get payslips by employee ID", description = "Returns a list of payslips for a specific employee. Accessible by ADMIN, MANAGER, or the employee themselves.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved payslips"),
        @ApiResponse(responseCode = "404", description = "Employee not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or (hasRole('EMPLOYEE') and #employeeId == authentication.principal.id)")
    public ResponseEntity<List<PayslipDto>> getPayslipsByEmployeeId(
            @Parameter(description = "ID of the employee to retrieve payslips for") @PathVariable Long employeeId) {
        return employeeRepository.findById(employeeId)
                .map(employee -> {
                    List<PayslipDto> payslips = payslipRepository.findByEmployee(employee).stream()
                            .map(this::convertToDto)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(payslips);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get payslip by employee ID, month, and year", description = "Returns a specific payslip for an employee. Accessible by ADMIN, MANAGER, or the employee themselves.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved payslip"),
        @ApiResponse(responseCode = "404", description = "Employee or payslip not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    @GetMapping("/employee/{employeeId}/month/{month}/year/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or (hasRole('EMPLOYEE') and #employeeId == authentication.principal.id)")
    public ResponseEntity<PayslipDto> getPayslipByEmployeeAndMonthAndYear(
            @Parameter(description = "ID of the employee") @PathVariable Long employeeId, 
            @Parameter(description = "Month (1-12)") @PathVariable Integer month, 
            @Parameter(description = "Year (e.g., 2025)") @PathVariable Integer year) {
        return employeeRepository.findById(employeeId)
                .map(employee -> payslipRepository.findByEmployeeAndMonthAndYear(employee, month, year)
                        .map(this::convertToDto)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get current employee's payslips", description = "Returns all payslips for the currently authenticated employee.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved payslips"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires EMPLOYEE role")
    })
    @GetMapping("/current")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<PayslipDto>> getCurrentEmployeePayslips(Authentication authentication) {
        Employee employee = (Employee) authentication.getPrincipal();
        List<PayslipDto> payslips = payslipRepository.findByEmployee(employee).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payslips);
    }

    @Operation(summary = "Get current employee's payslip by month and year", description = "Returns a specific payslip for the currently authenticated employee.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved payslip"),
        @ApiResponse(responseCode = "404", description = "Payslip not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires EMPLOYEE role")
    })
    @GetMapping("/current/month/{month}/year/{year}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<PayslipDto> getCurrentEmployeePayslipByMonthAndYear(
            Authentication authentication, 
            @Parameter(description = "Month (1-12)") @PathVariable Integer month, 
            @Parameter(description = "Year (e.g., 2025)") @PathVariable Integer year) {
        Employee employee = (Employee) authentication.getPrincipal();
        return payslipRepository.findByEmployeeAndMonthAndYear(employee, month, year)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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

    @Operation(summary = "Export payslips to CSV", description = "Exports payslips to a CSV file that can be opened in Excel. Accessible by ADMIN, MANAGER, or the employee themselves.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated CSV file"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> exportPayslipsToCsv() {
        List<Payslip> payslips = payslipRepository.findAll();
        return generateCsvResponse(payslips, "all_payslips.csv");
    }

    @Operation(summary = "Export employee's payslips to CSV", description = "Exports an employee's payslips to a CSV file that can be opened in Excel. Accessible by ADMIN, MANAGER, or the employee themselves.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated CSV file"),
        @ApiResponse(responseCode = "404", description = "Employee not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    @GetMapping("/employee/{employeeId}/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or (hasRole('EMPLOYEE') and #employeeId == authentication.principal.id)")
    public ResponseEntity<byte[]> exportEmployeePayslipsToCsv(
            @Parameter(description = "ID of the employee") @PathVariable Long employeeId) {
        return employeeRepository.findById(employeeId)
                .map(employee -> {
                    List<Payslip> payslips = payslipRepository.findByEmployee(employee);
                    return generateCsvResponse(payslips, "payslips_" + employee.getCode() + ".csv");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Export current employee's payslips to CSV", description = "Exports the current employee's payslips to a CSV file that can be opened in Excel. Accessible by EMPLOYEE role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated CSV file"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires EMPLOYEE role")
    })
    @GetMapping("/current/export/csv")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<byte[]> exportCurrentEmployeePayslipsToCsv(Authentication authentication) {
        Employee employee = (Employee) authentication.getPrincipal();
        List<Payslip> payslips = payslipRepository.findByEmployee(employee);
        return generateCsvResponse(payslips, "my_payslips.csv");
    }

    private ResponseEntity<byte[]> generateCsvResponse(List<Payslip> payslips, String filename) {
        StringBuilder csvContent = new StringBuilder();

        // Add CSV header
        csvContent.append("ID,Employee Code,Employee Name,Base Salary,Housing,Transport,Tax,Pension,Medical Insurance,Others,Gross Salary,Net Salary,Month,Year,Status\n");

        // Add data rows
        for (Payslip payslip : payslips) {
            Employee employee = payslip.getEmployee();
            csvContent.append(String.format("%d,%s,%s %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%d,%d,%s\n",
                    payslip.getId(),
                    employee.getCode(),
                    employee.getFirstName(),
                    employee.getLastName(),
                    payslip.getBaseSalary(),
                    payslip.getHouseAmount(),
                    payslip.getTransportAmount(),
                    payslip.getEmployeeTaxedAmount(),
                    payslip.getPensionAmount(),
                    payslip.getMedicalInsuranceAmount(),
                    payslip.getOtherTaxedAmount(),
                    payslip.getGrossSalary(),
                    payslip.getNetSalary(),
                    payslip.getMonth(),
                    payslip.getYear(),
                    payslip.getStatus()
            ));
        }

        // Set up response headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", filename);

        // Convert CSV content to byte array
        byte[] csvBytes = csvContent.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentLength(csvBytes.length)
                .body(csvBytes);
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
