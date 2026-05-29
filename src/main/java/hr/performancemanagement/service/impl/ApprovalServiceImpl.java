package hr.performancemanagement.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Approval;
import hr.performancemanagement.repository.ApprovalRepository;
import org.springframework.beans.factory.annotation.Autowired;


@Service
public class ApprovalServiceImpl implements hr.performancemanagement.service.api.ApprovalService {
    private static final Logger log = LoggerFactory.getLogger(ApprovalServiceImpl.class);

    @Autowired
    ApprovalRepository approvalRepository;

    @Override
    public void addApproval(Approval approval) {
       try {
           approvalRepository.save(approval);
           log.info("Approval added successfully for scorecard ID: {}", approval.getScorecard() != null ? approval.getScorecard().getId() : "null");
       }catch (Exception e){
           log.error("Error saving approval: {}", approval != null ? approval.getId() : "null", e);
           throw new RuntimeException("Failed to save approval", e);
       }
    }
}
