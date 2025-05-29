package rw.gov.epms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import rw.gov.epms.dto.employment.EmploymentRequest;
import rw.gov.epms.model.Employee;
import rw.gov.epms.model.Employee.EmployeeStatus;
import rw.gov.epms.model.Employment;
import rw.gov.epms.model.Employment.EmploymentStatus;
import rw.gov.epms.model.Role;
import rw.gov.epms.repository.EmployeeRepository;
import rw.gov.epms.repository.EmploymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EmploymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmploymentRepository employmentRepository;

    private Employee adminEmployee;
    private Employee managerEmployee;
    private Employee regularEmployee;
    private Employment employment;

    @BeforeEach
    void setUp() {
        // Clear the database before each test
        employmentRepository.deleteAll();
        employeeRepository.deleteAll();

        // Create test employees with different roles
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(Role.ROLE_ADMIN);

        Set<Role> managerRoles = new HashSet<>();
        managerRoles.add(Role.ROLE_MANAGER);

        Set<Role> employeeRoles = new HashSet<>();
        employeeRoles.add(Role.ROLE_EMPLOYEE);

        // Create an admin employee
        adminEmployee = createTestEmployee("admin@example.com", "Admin", "User", adminRoles);

        // Create a manager employee
        managerEmployee = createTestEmployee("manager@example.com", "Manager", "User", managerRoles);

        // Create a regular employee
        regularEmployee = createTestEmployee("employee@example.com", "Regular", "User", employeeRoles);

        // Create a test employment
        employment = Employment.builder()
                .code("EMP-TEST12345")
                .employee(regularEmployee)
                .department("IT")
                .position("Developer")
                .baseSalary(new BigDecimal("50000.00"))
                .status(EmploymentStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 1, 1))
                .build();

        employment = employmentRepository.save(employment);
    }

    private Employee createTestEmployee(String email, String firstName, String lastName, Set<Role> roles) {
        Employee employee = Employee.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .password("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG") // "password" encoded
                .mobile("+250781234567")
                .status(EmployeeStatus.ACTIVE)
                .code("EMP" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .roles(roles)
                .build();

        return employeeRepository.save(employee);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testGetAllEmployments() throws Exception {
        // Perform the get request
        ResultActions response = mockMvc.perform(get("/api/v1/employments")
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(1)))
                .andExpect(jsonPath("$[0].code", is(employment.getCode())))
                .andExpect(jsonPath("$[0].department", is(employment.getDepartment())))
                .andExpect(jsonPath("$[0].position", is(employment.getPosition())));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testGetEmploymentById() throws Exception {
        // Perform the get request
        ResultActions response = mockMvc.perform(get("/api/v1/employments/{id}", employment.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(employment.getId().intValue())))
                .andExpect(jsonPath("$.code", is(employment.getCode())))
                .andExpect(jsonPath("$.department", is(employment.getDepartment())))
                .andExpect(jsonPath("$.position", is(employment.getPosition())));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = {"EMPLOYEE"})
    void testGetEmploymentsByEmployeeId() throws Exception {
        // Perform the get request
        ResultActions response = mockMvc.perform(get("/api/v1/employments/employee/{employeeId}", regularEmployee.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(1)))
                .andExpect(jsonPath("$[0].code", is(employment.getCode())))
                .andExpect(jsonPath("$[0].department", is(employment.getDepartment())))
                .andExpect(jsonPath("$[0].position", is(employment.getPosition())));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = {"EMPLOYEE"})
    void testEmployeeCannotAccessOtherEmployeeEmployments() throws Exception {
        // Perform the get request
        ResultActions response = mockMvc.perform(get("/api/v1/employments/employee/{employeeId}", adminEmployee.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void testCreateEmployment() throws Exception {
        // Create an employment request
        EmploymentRequest employmentRequest = EmploymentRequest.builder()
                .employeeId(managerEmployee.getId())
                .department("HR")
                .position("Manager")
                .baseSalary(new BigDecimal("60000.00"))
                .status(EmploymentStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 2, 1))
                .build();

        // Perform the create request
        ResultActions response = mockMvc.perform(post("/api/v1/employments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(employmentRequest)));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", notNullValue()))
                .andExpect(jsonPath("$.department", is("HR")))
                .andExpect(jsonPath("$.position", is("Manager")))
                .andExpect(jsonPath("$.baseSalary", is(60000.00)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testUpdateEmployment() throws Exception {
        // Create an update request
        EmploymentRequest updateRequest = EmploymentRequest.builder()
                .employeeId(regularEmployee.getId())
                .department("Engineering")
                .position("Senior Developer")
                .baseSalary(new BigDecimal("70000.00"))
                .status(EmploymentStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 1, 1))
                .build();

        // Perform the update request
        ResultActions response = mockMvc.perform(put("/api/v1/employments/{id}", employment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(employment.getId().intValue())))
                .andExpect(jsonPath("$.department", is("Engineering")))
                .andExpect(jsonPath("$.position", is("Senior Developer")))
                .andExpect(jsonPath("$.baseSalary", is(70000.00)));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = {"EMPLOYEE"})
    void testEmployeeCannotCreateEmployment() throws Exception {
        // Create an employment request
        EmploymentRequest employmentRequest = EmploymentRequest.builder()
                .employeeId(regularEmployee.getId())
                .department("Fake")
                .position("Hacker")
                .baseSalary(new BigDecimal("100000.00"))
                .status(EmploymentStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 3, 1))
                .build();

        // Perform the create request
        ResultActions response = mockMvc.perform(post("/api/v1/employments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(employmentRequest)));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testDeleteEmployment() throws Exception {
        // Perform the delete request
        ResultActions response = mockMvc.perform(delete("/api/v1/employments/{id}", employment.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk());

        // Verify the employment was deleted
        mockMvc.perform(get("/api/v1/employments/{id}", employment.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void testManagerCannotDeleteEmployment() throws Exception {
        // Perform the delete request
        ResultActions response = mockMvc.perform(delete("/api/v1/employments/{id}", employment.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isForbidden());
    }
}
