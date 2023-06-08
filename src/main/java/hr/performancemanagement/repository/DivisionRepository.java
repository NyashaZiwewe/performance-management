package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Division;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DivisionRepository extends JpaRepository<Division, Long> {
    List<Division> findDivisionsByClientId(long clientId);
}
