package rw.gov.epms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.gov.epms.dto.auth.JwtResponse;
import rw.gov.epms.dto.auth.LoginRequest;
import rw.gov.epms.dto.auth.RegisterRequest;
import rw.gov.epms.model.Employee;
import rw.gov.epms.model.Employee.EmployeeStatus;
import rw.gov.epms.model.Role;
import rw.gov.epms.repository.EmployeeRepository;
import rw.gov.epms.security.JwtTokenProvider;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API for login and registration")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Operation(
        summary = "Authenticate user", 
        description = "Authenticates a user and returns a JWT token",
        security = {}  // Override global security - this endpoint doesn't require authentication
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated",
                content = @Content(schema = @Schema(implementation = JwtResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        Employee employee = (Employee) authentication.getPrincipal();

        return ResponseEntity.ok(JwtResponse.builder()
                .token(jwt)
                .id(employee.getId())
                .code(employee.getCode())
                .email(employee.getEmail())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .roles(employee.getRoles())
                .build());
    }

    @Operation(
        summary = "Register new user", 
        description = "Registers a new user in the system",
        security = {}  // Override global security - this endpoint doesn't require authentication
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Email already in use or invalid input")
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if (employeeRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Email is already in use!");
        }

        if (employeeRepository.existsByMobile(registerRequest.getMobile())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Mobile number is already in use!");
        }

        // Create new employee's account
        Employee employee = Employee.builder()
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .mobile(registerRequest.getMobile())
                .dateOfBirth(registerRequest.getDateOfBirth())
                .status(EmployeeStatus.ACTIVE)
                .code(generateEmployeeCode())
                .build();

        Set<Role> roles = registerRequest.getRoles();
        if (roles == null || roles.isEmpty()) {
            roles = new HashSet<>();
            roles.add(Role.ROLE_EMPLOYEE);
        }

        employee.setRoles(roles);
        Employee savedEmployee = employeeRepository.save(employee);

        return ResponseEntity.ok("User registered successfully!");
    }

    private String generateEmployeeCode() {
        return "EMP" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
