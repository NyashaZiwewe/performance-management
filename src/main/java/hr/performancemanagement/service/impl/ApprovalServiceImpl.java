package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Approval;
import hr.performancemanagement.repository.ApprovalRepository;
import org.springframework.beans.factory.annotation.Autowired;


@Service
public class ApprovalServiceImpl implements hr.performancemanagement.service.api.ApprovalService {

    @Autowired
    ApprovalRepository approvalRepository;

    @Override
    public void addApproval(Approval approval) {
       try {
           approvalRepository.save(approval);
       }catch (Exception e){
           e.printStackTrace();
       }
    }
}
