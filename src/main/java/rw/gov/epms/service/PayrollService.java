package rw.gov.epms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.gov.epms.model.Deduction;
import rw.gov.epms.model.Employee;
import rw.gov.epms.model.Employment;
import rw.gov.epms.model.Message;
import rw.gov.epms.model.Payslip;
import rw.gov.epms.model.Payslip.PayslipStatus;
import rw.gov.epms.repository.DeductionRepository;
import rw.gov.epms.repository.EmployeeRepository;
import rw.gov.epms.repository.EmploymentRepository;
import rw.gov.epms.repository.PayslipRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final EmploymentRepository employmentRepository;
    private final DeductionRepository deductionRepository;
    private final PayslipRepository payslipRepository;
    private final MessageService messageService;

    @Transactional
    public List<Payslip> generatePayroll(Integer month, Integer year) {
        // Check if payroll already exists for the given month and year
        List<Payslip> existingPayslips = payslipRepository.findByMonthAndYear(month, year);
        if (!existingPayslips.isEmpty()) {
            throw new IllegalStateException("Payroll already generated for " + month + "/" + year);
        }

        // Get all active employees
        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(employee -> employee.getStatus() == Employee.EmployeeStatus.ACTIVE)
                .collect(Collectors.toList());

        // Get all deductions
        List<Deduction> allDeductions = deductionRepository.findAll();

        // Group deductions by type
        Map<String, Deduction> deductionMap = allDeductions.stream()
                .collect(Collectors.toMap(Deduction::getDeductionName, deduction -> deduction));

        List<Payslip> generatedPayslips = new ArrayList<>();

        for (Employee employee : activeEmployees) {
            // Get active employment for the employee
            List<Employment> activeEmployments = employmentRepository.findByEmployeeAndStatus(
                    employee, Employment.EmploymentStatus.ACTIVE);

            if (activeEmployments.isEmpty()) {
                log.info("No active employment found for employee: {}", employee.getCode());
                continue;
            }

            // Use the first active employment (assuming an employee has only one active employment)
            Employment employment = activeEmployments.get(0);
            BigDecimal baseSalary = employment.getBaseSalary();

            // Calculate deductions and additions
            BigDecimal houseAmount = calculatePercentage(baseSalary, deductionMap.get("Housing"));
            BigDecimal transportAmount = calculatePercentage(baseSalary, deductionMap.get("Transport"));
            BigDecimal employeeTaxAmount = calculatePercentage(baseSalary, deductionMap.get("EmployeeTax"));
            BigDecimal pensionAmount = calculatePercentage(baseSalary, deductionMap.get("Pension"));
            BigDecimal medicalInsuranceAmount = calculatePercentage(baseSalary, deductionMap.get("MedicalInsurance"));
            BigDecimal otherAmount = calculatePercentage(baseSalary, deductionMap.get("Others"));

            // Calculate gross and net salary
            BigDecimal grossSalary = baseSalary.add(houseAmount).add(transportAmount);
            BigDecimal netSalary = grossSalary.subtract(employeeTaxAmount)
                    .subtract(pensionAmount)
                    .subtract(medicalInsuranceAmount)
                    .subtract(otherAmount);

            // Create payslip
            Payslip payslip = Payslip.builder()
                    .employee(employee)
                    .baseSalary(baseSalary)
                    .houseAmount(houseAmount)
                    .transportAmount(transportAmount)
                    .employeeTaxedAmount(employeeTaxAmount)
                    .pensionAmount(pensionAmount)
                    .medicalInsuranceAmount(medicalInsuranceAmount)
                    .otherTaxedAmount(otherAmount)
                    .grossSalary(grossSalary)
                    .netSalary(netSalary)
                    .month(month)
                    .year(year)
                    .status(PayslipStatus.PENDING)
                    .build();

            generatedPayslips.add(payslipRepository.save(payslip));
        }

        return generatedPayslips;
    }

    @Transactional
    public List<Payslip> approvePayroll(Integer month, Integer year) {
        List<Payslip> pendingPayslips = payslipRepository.findByMonthAndYearAndStatus(
                month, year, PayslipStatus.PENDING);

        if (pendingPayslips.isEmpty()) {
            throw new IllegalStateException("No pending payslips found for " + month + "/" + year);
        }

        List<Payslip> approvedPayslips = new ArrayList<>();

        for (Payslip payslip : pendingPayslips) {
            payslip.setStatus(PayslipStatus.PAID);
            approvedPayslips.add(payslipRepository.save(payslip));
        }

        // Create and send messages for approved payslips
        List<Message> messages = messageService.createMessagesForApprovedPayslips(approvedPayslips);
        messageService.sendMessages(messages);

        log.info("Sent payroll notification emails to {} employees for {}/{}", approvedPayslips.size(), month, year);

        return approvedPayslips;
    }

    private BigDecimal calculatePercentage(BigDecimal amount, Deduction deduction) {
        if (deduction == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal percentage = deduction.getPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal result = amount.multiply(percentage).setScale(2, RoundingMode.HALF_UP);

        // If it's an addition (like housing, transport), return positive amount
        // If it's a deduction (like tax, pension), return positive amount as well
        // The deduction will be subtracted from gross salary in the calling method
        return result;
    }
}
