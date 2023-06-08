package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Approval;
import hr.performancemanagement.repository.ApprovalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApprovalService {

    @Autowired
    ApprovalRepository approvalRepository;

    public void addApproval(Approval approval) {
       try {
           approvalRepository.save(approval);
       }catch (Exception e){
           e.printStackTrace();
       }
    }
}
