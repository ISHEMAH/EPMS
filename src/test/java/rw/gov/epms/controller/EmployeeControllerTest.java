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
import rw.gov.epms.dto.auth.RegisterRequest;
import rw.gov.epms.dto.employee.EmployeeUpdateDto;
import rw.gov.epms.model.Employee;
import rw.gov.epms.model.Employee.EmployeeStatus;
import rw.gov.epms.model.Role;
import rw.gov.epms.repository.EmployeeRepository;

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
public class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Employee adminEmployee;
    private Employee managerEmployee;
    private Employee regularEmployee;

    @BeforeEach
    void setUp() {
        // Clear the database before each test
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
    void testGetAllEmployees() throws Exception {
        // Perform the get request
        ResultActions response = mockMvc.perform(get("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(3)))
                .andExpect(jsonPath("$[0].email", notNullValue()))
                .andExpect(jsonPath("$[1].email", notNullValue()))
                .andExpect(jsonPath("$[2].email", notNullValue()));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testGetEmployeeById() throws Exception {
        // Perform the get request
        ResultActions response = mockMvc.perform(get("/api/v1/employees/{id}", regularEmployee.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(regularEmployee.getId().intValue())))
                .andExpect(jsonPath("$.email", is(regularEmployee.getEmail())))
                .andExpect(jsonPath("$.firstName", is(regularEmployee.getFirstName())))
                .andExpect(jsonPath("$.lastName", is(regularEmployee.getLastName())));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = {"EMPLOYEE"})
    void testEmployeeCanAccessOwnProfile() throws Exception {
        // Perform the get request
        ResultActions response = mockMvc.perform(get("/api/v1/employees/{id}", regularEmployee.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(regularEmployee.getId().intValue())))
                .andExpect(jsonPath("$.email", is(regularEmployee.getEmail())));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = {"EMPLOYEE"})
    void testEmployeeCannotAccessOtherProfiles() throws Exception {
        // Perform the get request
        ResultActions response = mockMvc.perform(get("/api/v1/employees/{id}", adminEmployee.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testUpdateEmployee() throws Exception {
        // Create an update request
        EmployeeUpdateDto updateDto = EmployeeUpdateDto.builder()
                .firstName("Updated")
                .lastName("Name")
                .email("updated.email@example.com")
                .mobile("+250789876543")
                .build();

        // Perform the update request
        ResultActions response = mockMvc.perform(put("/api/v1/employees/{id}", regularEmployee.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Updated")))
                .andExpect(jsonPath("$.lastName", is("Name")))
                .andExpect(jsonPath("$.email", is("updated.email@example.com")))
                .andExpect(jsonPath("$.mobile", is("+250789876543")));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = {"EMPLOYEE"})
    void testEmployeeCannotUpdateProfiles() throws Exception {
        // Create an update request
        EmployeeUpdateDto updateDto = EmployeeUpdateDto.builder()
                .firstName("Hacked")
                .build();

        // Perform the update request
        ResultActions response = mockMvc.perform(put("/api/v1/employees/{id}", regularEmployee.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testDeleteEmployee() throws Exception {
        // Perform the delete request
        ResultActions response = mockMvc.perform(delete("/api/v1/employees/{id}", regularEmployee.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk());

        // Verify the employee was deleted
        mockMvc.perform(get("/api/v1/employees/{id}", regularEmployee.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void testManagerCannotDeleteEmployee() throws Exception {
        // Perform the delete request
        ResultActions response = mockMvc.perform(delete("/api/v1/employees/{id}", regularEmployee.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isForbidden());
    }
}
