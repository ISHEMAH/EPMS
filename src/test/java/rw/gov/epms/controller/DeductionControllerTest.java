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
import rw.gov.epms.dto.deduction.DeductionRequest;
import rw.gov.epms.model.Deduction;
import rw.gov.epms.repository.DeductionRepository;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DeductionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeductionRepository deductionRepository;

    private Deduction employeeTaxDeduction;
    private Deduction pensionDeduction;

    @BeforeEach
    void setUp() {
        // Clear the database before each test
        deductionRepository.deleteAll();

        // Create test deductions
        employeeTaxDeduction = Deduction.builder()
                .code("DED-TAX12345")
                .deductionName("Employee Tax")
                .percentage(new BigDecimal("30.00"))
                .isAddition(false)
                .build();

        pensionDeduction = Deduction.builder()
                .code("DED-PEN12345")
                .deductionName("Pension")
                .percentage(new BigDecimal("6.00"))
                .isAddition(false)
                .build();

        employeeTaxDeduction = deductionRepository.save(employeeTaxDeduction);
        pensionDeduction = deductionRepository.save(pensionDeduction);
    }

    @Test
    void testGetAllDeductions() throws Exception {
        // Perform the get request
        ResultActions response = mockMvc.perform(get("/api/v1/deductions")
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(2)))
                .andExpect(jsonPath("$[0].code", notNullValue()))
                .andExpect(jsonPath("$[1].code", notNullValue()));
    }

    @Test
    void testGetDeductionById() throws Exception {
        // Perform the get request
        ResultActions response = mockMvc.perform(get("/api/v1/deductions/{id}", employeeTaxDeduction.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(employeeTaxDeduction.getId().intValue())))
                .andExpect(jsonPath("$.code", is(employeeTaxDeduction.getCode())))
                .andExpect(jsonPath("$.deductionName", is(employeeTaxDeduction.getDeductionName())))
                .andExpect(jsonPath("$.percentage", is(30.00)));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testCreateDeduction() throws Exception {
        // Create a deduction request
        DeductionRequest deductionRequest = DeductionRequest.builder()
                .deductionName("Medical Insurance")
                .percentage(new BigDecimal("5.00"))
                .isAddition(false)
                .build();

        // Perform the create request
        ResultActions response = mockMvc.perform(post("/api/v1/deductions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deductionRequest)));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", notNullValue()))
                .andExpect(jsonPath("$.deductionName", is("Medical Insurance")))
                .andExpect(jsonPath("$.percentage", is(5.00)))
                .andExpect(jsonPath("$.addition", is(false)));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testCreateDeductionWithExistingName() throws Exception {
        // Create a deduction request with an existing name
        DeductionRequest deductionRequest = DeductionRequest.builder()
                .deductionName("Employee Tax") // Already exists
                .percentage(new BigDecimal("25.00"))
                .isAddition(false)
                .build();

        // Perform the create request
        ResultActions response = mockMvc.perform(post("/api/v1/deductions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deductionRequest)));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", is("Deduction with name Employee Tax already exists")));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void testUpdateDeduction() throws Exception {
        // Create an update request
        DeductionRequest updateRequest = DeductionRequest.builder()
                .deductionName("Pension Updated")
                .percentage(new BigDecimal("7.00"))
                .isAddition(false)
                .build();

        // Perform the update request
        ResultActions response = mockMvc.perform(put("/api/v1/deductions/{id}", pensionDeduction.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(pensionDeduction.getId().intValue())))
                .andExpect(jsonPath("$.deductionName", is("Pension Updated")))
                .andExpect(jsonPath("$.percentage", is(7.00)));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = {"EMPLOYEE"})
    void testEmployeeCannotCreateDeduction() throws Exception {
        // Create a deduction request
        DeductionRequest deductionRequest = DeductionRequest.builder()
                .deductionName("Fake Deduction")
                .percentage(new BigDecimal("100.00"))
                .isAddition(true)
                .build();

        // Perform the create request
        ResultActions response = mockMvc.perform(post("/api/v1/deductions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deductionRequest)));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void testDeleteDeduction() throws Exception {
        // Perform the delete request
        ResultActions response = mockMvc.perform(delete("/api/v1/deductions/{id}", pensionDeduction.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isOk());

        // Verify the deduction was deleted
        mockMvc.perform(get("/api/v1/deductions/{id}", pensionDeduction.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    void testManagerCannotDeleteDeduction() throws Exception {
        // Perform the delete request
        ResultActions response = mockMvc.perform(delete("/api/v1/deductions/{id}", pensionDeduction.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Verify the response
        response.andDo(print())
                .andExpect(status().isForbidden());
    }
}