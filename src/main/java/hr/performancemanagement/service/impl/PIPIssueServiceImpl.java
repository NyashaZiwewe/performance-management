package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Issue;
import hr.performancemanagement.entities.PIPIssue;
import hr.performancemanagement.repository.IssueRepository;
import hr.performancemanagement.repository.PIPIssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class PIPIssueServiceImpl implements hr.performancemanagement.service.api.PIPIssueService {
    @Autowired
    PIPIssueRepository pipIssueRepository;

    @Override
    public PIPIssue getPIPIssueById(long id){

        PIPIssue pipIssue = pipIssueRepository.findPIPIssueById(id);
        return pipIssue;
    }

    @Override
    public List<PIPIssue> listAllPIPIssues(long planId){
        List<PIPIssue> pipIssueList = new ArrayList<>();
        pipIssueRepository.findPIPIssuesByPerformanceImprovementPlan_Id(planId).forEach(pipIssue -> pipIssueList.add(pipIssue));
        return pipIssueList;
    }


    @Override
    public void savePIPIssue(PIPIssue pipIssue) {
        pipIssueRepository.save(pipIssue);
    }

    @Transactional
    @Override
    public void deletePIPIssue(PIPIssue pipIssue){
        pipIssueRepository.delete(pipIssue);
    }
}
