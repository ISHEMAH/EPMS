package rw.gov.epms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.gov.epms.model.Employee;
import rw.gov.epms.model.Payslip;
import rw.gov.epms.model.Payslip.PayslipStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    List<Payslip> findByEmployee(Employee employee);
    List<Payslip> findByEmployeeAndStatus(Employee employee, PayslipStatus status);
    List<Payslip> findByMonthAndYear(Integer month, Integer year);
    List<Payslip> findByMonthAndYearAndStatus(Integer month, Integer year, PayslipStatus status);
    Optional<Payslip> findByEmployeeAndMonthAndYear(Employee employee, Integer month, Integer year);
    boolean existsByEmployeeAndMonthAndYear(Employee employee, Integer month, Integer year);
}