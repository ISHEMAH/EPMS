package rw.gov.epms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import rw.gov.epms.dto.employee.EmployeeDto;
import rw.gov.epms.dto.employee.EmployeeUpdateDto;
import rw.gov.epms.model.Employee;
import rw.gov.epms.repository.EmployeeRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Management", description = "APIs for managing employees")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;

    @Operation(summary = "Get all employees", description = "Returns a list of all employees. Accessible by ADMIN and MANAGER roles.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of employees",
                content = @Content(schema = @Schema(implementation = EmployeeDto.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or MANAGER role")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<EmployeeDto>> getAllEmployees() {
        List<EmployeeDto> employees = employeeRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(employees);
    }

    @Operation(summary = "Get current employee", description = "Returns the currently authenticated employee's data.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved employee data"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not authenticated")
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeDto> getCurrentEmployee(Authentication authentication) {
        Employee employee = (Employee) authentication.getPrincipal();
        return ResponseEntity.ok(convertToDto(employee));
    }

    @Operation(summary = "Get employee by ID", description = "Returns an employee by ID. Accessible by ADMIN, MANAGER, or the employee themselves.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved employee"),
        @ApiResponse(responseCode = "404", description = "Employee not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or (hasRole('EMPLOYEE') and #id == authentication.principal.id)")
    public ResponseEntity<EmployeeDto> getEmployeeById(
            @Parameter(description = "ID of the employee to retrieve") @PathVariable Long id) {
        return employeeRepository.findById(id)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update employee", description = "Updates an employee's information. Accessible by ADMIN and MANAGER roles.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated employee"),
        @ApiResponse(responseCode = "404", description = "Employee not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or MANAGER role"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateEmployee(
            @Parameter(description = "ID of the employee to update") @PathVariable Long id, 
            @Valid @RequestBody EmployeeUpdateDto employeeUpdateDto) {
        // Check for email uniqueness if email is being updated
        if (employeeUpdateDto.getEmail() != null) {
            Employee existingEmployee = employeeRepository.findByEmail(employeeUpdateDto.getEmail()).orElse(null);
            if (existingEmployee != null && !existingEmployee.getId().equals(id)) {
                return ResponseEntity.badRequest().body("Error: Email is already in use!");
            }
        }

        // Check for mobile uniqueness if mobile is being updated
        if (employeeUpdateDto.getMobile() != null) {
            Employee existingEmployee = employeeRepository.findByMobile(employeeUpdateDto.getMobile()).orElse(null);
            if (existingEmployee != null && !existingEmployee.getId().equals(id)) {
                return ResponseEntity.badRequest().body("Error: Mobile number is already in use!");
            }
        }
        return employeeRepository.findById(id)
                .map(employee -> {
                    if (employeeUpdateDto.getFirstName() != null) {
                        employee.setFirstName(employeeUpdateDto.getFirstName());
                    }
                    if (employeeUpdateDto.getLastName() != null) {
                        employee.setLastName(employeeUpdateDto.getLastName());
                    }
                    if (employeeUpdateDto.getEmail() != null) {
                        // Check if email is already in use by another employee
                        if (!employee.getEmail().equals(employeeUpdateDto.getEmail()) && 
                            employeeRepository.existsByEmail(employeeUpdateDto.getEmail())) {
                            return ResponseEntity.badRequest().body("Error: Email is already in use!");
                        }
                        employee.setEmail(employeeUpdateDto.getEmail());
                    }
                    if (employeeUpdateDto.getMobile() != null) {
                        // Check if mobile is already in use by another employee
                        if (!employee.getMobile().equals(employeeUpdateDto.getMobile()) && 
                            employeeRepository.existsByMobile(employeeUpdateDto.getMobile())) {
                            return ResponseEntity.badRequest().body("Error: Mobile number is already in use!");
                        }
                        employee.setMobile(employeeUpdateDto.getMobile());
                    }
                    if (employeeUpdateDto.getDateOfBirth() != null) {
                        employee.setDateOfBirth(employeeUpdateDto.getDateOfBirth());
                    }
                    if (employeeUpdateDto.getStatus() != null) {
                        employee.setStatus(employeeUpdateDto.getStatus());
                    }
                    if (employeeUpdateDto.getRoles() != null && !employeeUpdateDto.getRoles().isEmpty()) {
                        employee.setRoles(employeeUpdateDto.getRoles());
                    }

                    Employee updatedEmployee = employeeRepository.save(employee);
                    return ResponseEntity.ok(convertToDto(updatedEmployee));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete employee", description = "Deletes an employee. Accessible only by ADMIN role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted employee"),
        @ApiResponse(responseCode = "404", description = "Employee not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEmployee(
            @Parameter(description = "ID of the employee to delete") @PathVariable Long id) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    employeeRepository.delete(employee);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private EmployeeDto convertToDto(Employee employee) {
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
