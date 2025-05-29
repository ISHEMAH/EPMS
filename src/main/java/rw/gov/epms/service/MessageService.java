package rw.gov.epms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.gov.epms.model.Employee;
import rw.gov.epms.model.Message;
import rw.gov.epms.model.Payslip;
import rw.gov.epms.repository.MessageRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final JavaMailSender emailSender;
    
    private static final String INSTITUTION_NAME = "Rwanda Government";
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM/yyyy", Locale.ENGLISH);

    @Transactional
    public List<Message> createMessagesForApprovedPayslips(List<Payslip> approvedPayslips) {
        List<Message> messages = new ArrayList<>();
        
        for (Payslip payslip : approvedPayslips) {
            Employee employee = payslip.getEmployee();
            String monthYear = formatMonthYear(payslip.getMonth(), payslip.getYear());
            String messageContent = generateMessageContent(employee, payslip, monthYear);
            
            Message message = Message.builder()
                    .employee(employee)
                    .content(messageContent)
                    .monthYear(monthYear)
                    .createdAt(LocalDateTime.now())
                    .isSent(false)
                    .build();
            
            messages.add(messageRepository.save(message));
        }
        
        return messages;
    }
    
    @Transactional
    public void sendMessages(List<Message> messages) {
        for (Message message : messages) {
            try {
                sendEmail(message.getEmployee().getEmail(), "Salary Payment Notification", message.getContent());
                message.setSent(true);
                messageRepository.save(message);
                log.info("Email sent successfully to: {}", message.getEmployee().getEmail());
            } catch (Exception e) {
                log.error("Failed to send email to: {}", message.getEmployee().getEmail(), e);
            }
        }
    }
    
    private String generateMessageContent(Employee employee, Payslip payslip, String monthYear) {
        return String.format(
                "Dear %s, your salary for %s from %s amounting to %s has been credited to your account %s successfully.",
                employee.getFirstName(),
                monthYear,
                INSTITUTION_NAME,
                payslip.getNetSalary(),
                employee.getCode()
        );
    }
    
    private String formatMonthYear(int month, int year) {
        // Convert month (1-12) to Month enum (1-based)
        return java.time.Month.of(month).toString() + "/" + year;
    }
    
    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }
}