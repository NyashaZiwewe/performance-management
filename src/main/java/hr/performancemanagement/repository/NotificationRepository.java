package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findNotificationsByUser(long user);
    List<Notification> findNotificationsByStatus(String status);
}