package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Approval;
import hr.performancemanagement.repository.ApprovalRepository;

public interface ApprovalService {
    void addApproval(Approval approval);
}
