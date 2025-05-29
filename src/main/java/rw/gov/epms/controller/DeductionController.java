package rw.gov.epms.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.gov.epms.dto.deduction.DeductionDto;
import rw.gov.epms.dto.deduction.DeductionRequest;
import rw.gov.epms.model.Deduction;
import rw.gov.epms.repository.DeductionRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/deductions")
@RequiredArgsConstructor
public class DeductionController {

    private final DeductionRepository deductionRepository;

    @GetMapping
    public ResponseEntity<List<DeductionDto>> getAllDeductions() {
        List<DeductionDto> deductions = deductionRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(deductions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeductionDto> getDeductionById(@PathVariable Long id) {
        return deductionRepository.findById(id)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> createDeduction(@Valid @RequestBody DeductionRequest deductionRequest) {
        if (deductionRepository.existsByDeductionName(deductionRequest.getDeductionName())) {
            return ResponseEntity.badRequest().body("Deduction with name " + deductionRequest.getDeductionName() + " already exists");
        }

        Deduction deduction = Deduction.builder()
                .code(generateDeductionCode())
                .deductionName(deductionRequest.getDeductionName())
                .percentage(deductionRequest.getPercentage())
                .isAddition(deductionRequest.isAddition())
                .build();

        Deduction savedDeduction = deductionRepository.save(deduction);
        return ResponseEntity.ok(convertToDto(savedDeduction));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> updateDeduction(@PathVariable Long id, @Valid @RequestBody DeductionRequest deductionRequest) {
        return deductionRepository.findById(id)
                .map(deduction -> {
                    // Check if the name is being changed and if the new name already exists
                    if (!deduction.getDeductionName().equals(deductionRequest.getDeductionName()) &&
                            deductionRepository.existsByDeductionName(deductionRequest.getDeductionName())) {
                        return ResponseEntity.badRequest().body("Deduction with name " + deductionRequest.getDeductionName() + " already exists");
                    }

                    deduction.setDeductionName(deductionRequest.getDeductionName());
                    deduction.setPercentage(deductionRequest.getPercentage());
                    deduction.setAddition(deductionRequest.isAddition());

                    Deduction updatedDeduction = deductionRepository.save(deduction);
                    return ResponseEntity.ok(convertToDto(updatedDeduction));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDeduction(@PathVariable Long id) {
        return deductionRepository.findById(id)
                .map(deduction -> {
                    deductionRepository.delete(deduction);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private DeductionDto convertToDto(Deduction deduction) {
        return DeductionDto.builder()
                .id(deduction.getId())
                .code(deduction.getCode())
                .deductionName(deduction.getDeductionName())
                .percentage(deduction.getPercentage())
                .isAddition(deduction.isAddition())
                .build();
    }

    private String generateDeductionCode() {
        return "DED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}