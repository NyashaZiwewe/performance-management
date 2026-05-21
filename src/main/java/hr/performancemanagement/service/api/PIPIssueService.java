package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Issue;
import hr.performancemanagement.entities.PIPIssue;
import hr.performancemanagement.repository.IssueRepository;
import hr.performancemanagement.repository.PIPIssueRepository;
import java.util.ArrayList;
import java.util.List;

public interface PIPIssueService {
    PIPIssue getPIPIssueById(long id);
    List<PIPIssue> listAllPIPIssues(long planId);
    void savePIPIssue(PIPIssue pipIssue);
    void deletePIPIssue(PIPIssue pipIssue);
}
