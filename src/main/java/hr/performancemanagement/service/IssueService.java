package hr.performancemanagement.service;

import hr.performancemanagement.entities.ActionPlan;
import hr.performancemanagement.entities.Issue;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.repository.ActionPlanRepository;
import hr.performancemanagement.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class IssueService {
    @Autowired
    IssueRepository issueRepository;

    public Issue getIssueById(long id){

        Issue issue = issueRepository.findIssueById(id);
        return issue;
    }

    public List<Issue> listAllIssues(long planId){
        List<Issue> issueList = new ArrayList<>();
        issueRepository.findIssuesByActionPlan_Id(planId).forEach(issue -> issueList.add(issue));
        return issueList;
    }


    public void saveIssue(Issue issue) {
            issueRepository.save(issue);
    }

    @Transactional
    public void deleteIssue(Issue issue){
        issueRepository.delete(issue);
    }
}
