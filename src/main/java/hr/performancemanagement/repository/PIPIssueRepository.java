package hr.performancemanagement.repository;
import hr.performancemanagement.entities.PIPIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PIPIssueRepository extends JpaRepository<PIPIssue, Long> {

    List<PIPIssue> findPIPIssuesByPerformanceImprovementPlan_Id(long id);
    PIPIssue findPIPIssueById(long id);


}
