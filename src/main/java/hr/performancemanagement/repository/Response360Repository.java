package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Response360;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Response360Repository extends JpaRepository<Response360, Long> {
      List<Response360> findResponse360sByEmployeeId(int employeeId);
      List<Response360> findResponse360sByQuestionId(int questionId);
      List<Response360> findResponse360sByAssessorId(int assessorId);
}
