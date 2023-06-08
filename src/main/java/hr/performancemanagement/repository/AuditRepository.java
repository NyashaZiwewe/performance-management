package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Audit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<Audit, Long> {

    List<Audit> findAuditsByClientId(long clientId);
    List<Audit> findAuditsByUserId(long userId);
}
