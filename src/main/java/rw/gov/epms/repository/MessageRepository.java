package rw.gov.epms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.gov.epms.model.Employee;
import rw.gov.epms.model.Message;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByEmployee(Employee employee);
    List<Message> findByEmployeeAndMonthYear(Employee employee, String monthYear);
    List<Message> findByEmployeeAndIsSent(Employee employee, boolean isSent);
    List<Message> findByIsSent(boolean isSent);
}