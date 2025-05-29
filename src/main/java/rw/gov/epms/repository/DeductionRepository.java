package rw.gov.epms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.gov.epms.model.Deduction;

import java.util.Optional;

@Repository
public interface DeductionRepository extends JpaRepository<Deduction, Long> {
    Optional<Deduction> findByCode(String code);
    Optional<Deduction> findByDeductionName(String deductionName);
    boolean existsByCode(String code);
    boolean existsByDeductionName(String deductionName);
}