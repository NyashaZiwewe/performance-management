package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Step360;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Step360Repository extends JpaRepository<Step360, Long> {

}
