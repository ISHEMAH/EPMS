package rw.gov.epms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import rw.gov.epms.dto.auth.LoginRequest;
import rw.gov.epms.dto.auth.RegisterRequest;
import rw.gov.epms.model.Role;
import rw.gov.epms.repository.EmployeeRepository;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        // Clear the database before each test
        employeeRepository.deleteAll();
    }

    @Test
    void testRegisterUser() throws Exception {
        // Create a register request
        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_EMPLOYEE);

        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("password123")
                .mobile("+250781234567")
                .roles(roles)
                .build();

        // Perform the register request
        ResultActions response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is("User registered successfully!")));
    }

    @Test
    void testLoginUser() throws Exception {
        // First register a user
        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_EMPLOYEE);

        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .password("password123")
                .mobile("+250781234567")
                .roles(roles)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Create a login request
        LoginRequest loginRequest = LoginRequest.builder()
                .email("jane.doe@example.com")
                .password("password123")
                .build();

        // Perform the login request
        ResultActions response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.email", is("jane.doe@example.com")))
                .andExpect(jsonPath("$.firstName", is("Jane")))
                .andExpect(jsonPath("$.lastName", is("Doe")));
    }

    @Test
    void testRegisterUserWithExistingEmail() throws Exception {
        // First register a user
        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_EMPLOYEE);

        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice.smith@example.com")
                .password("password123")
                .mobile("+250781234567")
                .roles(roles)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Try to register another user with the same email
        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .firstName("Bob")
                .lastName("Smith")
                .email("alice.smith@example.com") // Same email
                .password("password456")
                .mobile("+250789876543")
                .roles(roles)
                .build();

        // Perform the register request
        ResultActions response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", is("Error: Email is already in use!")));
    }

    @Test
    void testLoginWithInvalidCredentials() throws Exception {
        // Create a login request with invalid credentials
        LoginRequest loginRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("wrongpassword")
                .build();

        // Perform the login request
        ResultActions response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
