package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Question360;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Question360Repository extends JpaRepository<Question360, Long> {
    List<Question360> findQuestion360sByPeriod(String period);
}
