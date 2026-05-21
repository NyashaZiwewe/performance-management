package hr.performancemanagement.repository;

import hr.performancemanagement.entities.EmailNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailNotificationLogRepository extends JpaRepository<EmailNotificationLog, Long> {

    List<EmailNotificationLog> findTop20ByRecipientEmailOrderByDateDesc(String recipientEmail);
}
