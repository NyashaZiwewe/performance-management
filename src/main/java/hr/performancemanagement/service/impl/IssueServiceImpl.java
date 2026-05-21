package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.ActionPlan;
import hr.performancemanagement.entities.Issue;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.repository.ActionPlanRepository;
import hr.performancemanagement.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class IssueServiceImpl implements hr.performancemanagement.service.api.IssueService {
    @Autowired
    IssueRepository issueRepository;

    @Override
    public Issue getIssueById(long id){

        Issue issue = issueRepository.findIssueById(id);
        return issue;
    }

    @Override
    public List<Issue> listAllIssues(long planId){
        List<Issue> issueList = new ArrayList<>();
        issueRepository.findIssuesByActionPlan_Id(planId).forEach(issue -> issueList.add(issue));
        return issueList;
    }


    @Override
    public void saveIssue(Issue issue) {
            issueRepository.save(issue);
    }

    @Transactional
    @Override
    public void deleteIssue(Issue issue){
        issueRepository.delete(issue);
    }
}
