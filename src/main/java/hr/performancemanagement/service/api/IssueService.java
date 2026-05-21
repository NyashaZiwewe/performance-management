package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.ActionPlan;
import hr.performancemanagement.entities.Issue;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.repository.ActionPlanRepository;
import hr.performancemanagement.repository.IssueRepository;
import java.util.ArrayList;
import java.util.List;

public interface IssueService {
    Issue getIssueById(long id);
    List<Issue> listAllIssues(long planId);
    void saveIssue(Issue issue);
    void deleteIssue(Issue issue);
}
