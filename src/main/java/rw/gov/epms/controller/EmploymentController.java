package rw.gov.epms.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.gov.epms.dto.employee.EmployeeDto;
import rw.gov.epms.dto.employment.EmploymentDto;
import rw.gov.epms.dto.employment.EmploymentRequest;
import rw.gov.epms.model.Employee;
import rw.gov.epms.model.Employment;
import rw.gov.epms.repository.EmployeeRepository;
import rw.gov.epms.repository.EmploymentRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/employments")
@RequiredArgsConstructor
public class EmploymentController {

    private final EmploymentRepository employmentRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<EmploymentDto>> getAllEmployments() {
        List<EmploymentDto> employments = employmentRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(employments);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<EmploymentDto> getEmploymentById(@PathVariable Long id) {
        return employmentRepository.findById(id)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or (hasRole('EMPLOYEE') and #employeeId == authentication.principal.id)")
    public ResponseEntity<List<EmploymentDto>> getEmploymentsByEmployeeId(@PathVariable Long employeeId) {
        return employeeRepository.findById(employeeId)
                .map(employee -> {
                    List<EmploymentDto> employments = employmentRepository.findByEmployee(employee).stream()
                            .map(this::convertToDto)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(employments);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> createEmployment(@Valid @RequestBody EmploymentRequest employmentRequest) {
        java.util.Optional<Employee> employeeOpt = employeeRepository.findById(employmentRequest.getEmployeeId());

        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            Employment employment = Employment.builder()
                    .code(generateEmploymentCode())
                    .employee(employee)
                    .department(employmentRequest.getDepartment())
                    .position(employmentRequest.getPosition())
                    .baseSalary(employmentRequest.getBaseSalary())
                    .status(employmentRequest.getStatus())
                    .joiningDate(employmentRequest.getJoiningDate())
                    .build();

            Employment savedEmployment = employmentRepository.save(employment);
            return ResponseEntity.ok().body(convertToDto(savedEmployment));
        } else {
            return ResponseEntity.badRequest().body("Employee not found with id: " + employmentRequest.getEmployeeId());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateEmployment(@PathVariable Long id, @Valid @RequestBody EmploymentRequest employmentRequest) {
        if (!employmentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        java.util.Optional<Employee> employeeOpt = employeeRepository.findById(employmentRequest.getEmployeeId());
        if (!employeeOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Employee not found with id: " + employmentRequest.getEmployeeId());
        }

        Employee employee = employeeOpt.get();
        java.util.Optional<Employment> employmentOpt = employmentRepository.findById(id);

        if (!employmentOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Employment employment = employmentOpt.get();
        employment.setEmployee(employee);
        employment.setDepartment(employmentRequest.getDepartment());
        employment.setPosition(employmentRequest.getPosition());
        employment.setBaseSalary(employmentRequest.getBaseSalary());
        employment.setStatus(employmentRequest.getStatus());
        employment.setJoiningDate(employmentRequest.getJoiningDate());

        Employment updatedEmployment = employmentRepository.save(employment);
        return ResponseEntity.ok().body(convertToDto(updatedEmployment));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEmployment(@PathVariable Long id) {
        return employmentRepository.findById(id)
                .map(employment -> {
                    employmentRepository.delete(employment);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private EmploymentDto convertToDto(Employment employment) {
        return EmploymentDto.builder()
                .id(employment.getId())
                .code(employment.getCode())
                .employee(convertEmployeeToDto(employment.getEmployee()))
                .department(employment.getDepartment())
                .position(employment.getPosition())
                .baseSalary(employment.getBaseSalary())
                .status(employment.getStatus())
                .joiningDate(employment.getJoiningDate())
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

    private String generateEmploymentCode() {
        return "EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
