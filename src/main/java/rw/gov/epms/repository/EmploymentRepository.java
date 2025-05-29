package rw.gov.epms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.gov.epms.model.Employment;
import rw.gov.epms.model.Employee;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmploymentRepository extends JpaRepository<Employment, Long> {
    List<Employment> findByEmployee(Employee employee);
    List<Employment> findByEmployeeAndStatus(Employee employee, Employment.EmploymentStatus status);
    Optional<Employment> findByCode(String code);
    boolean existsByCode(String code);
}