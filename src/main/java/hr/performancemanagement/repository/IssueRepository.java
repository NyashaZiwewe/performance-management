package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findIssuesByActionPlan_Id(long id);
    Issue findIssueById(long id);

}
