package rw.gov.epms.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.gov.epms.model.Employee.EmployeeStatus;
import rw.gov.epms.model.Role;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUpdateDto {
    @NotBlank(message = "First name is required if provided")
    private String firstName;

    @NotBlank(message = "Last name is required if provided")
    private String lastName;

    @Email(message = "Email should be valid")
    private String email;

    @Pattern(regexp = "^(\\+25078|078|\\+25079|079|\\+25073|073|\\+25072|072)[0-9]{7}$", message = "Mobile number should be in format +250789175211 or 0789175211")
    private String mobile;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Status is required if provided")
    private EmployeeStatus status;

    @NotNull(message = "Roles are required if provided")
    private Set<Role> roles;
}
