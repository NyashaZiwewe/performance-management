package hr.performancemanagement.service;

import hr.performancemanagement.entities.Evidence;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Target;
import hr.performancemanagement.repository.EvidenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvidenceService {

    @Autowired
    EvidenceRepository repository;

    public boolean existsEvidenceByTargetAndReportingDate(Target target, ReportingDate reportingDate) {
        return repository.existsEvidenceByTargetAndReportingDate(target, reportingDate);
    }

    public void saveEvidence(Evidence evidence) {
        Evidence existingEvidence = getLatestEvidence(evidence.getTarget(), evidence.getReportingDate());
        if (existingEvidence != null) {
            if(evidence.getAttachmentName() != null && !"".equalsIgnoreCase(evidence.getAttachmentName())) {
                existingEvidence.setAttachmentName(evidence.getAttachmentName());
            }
            if(evidence.getJustification() != null && !"".equalsIgnoreCase(evidence.getJustification())) {
                existingEvidence.setJustification(evidence.getJustification());
            }
            if(evidence.getEvidence() != null && !"".equalsIgnoreCase(evidence.getEvidence())) {
                existingEvidence.setEvidence(evidence.getEvidence());
            }
            repository.save(existingEvidence);
        }else {
           repository.save(evidence);
        }
    }

    private Evidence getLatestEvidence(Target target, ReportingDate reportingDate) {
        List<Evidence> evidenceList;
        if (target != null && target.getId() > 0 && reportingDate != null && reportingDate.getId() > 0) {
            evidenceList = repository.findEvidenceByTarget_IdAndReportingDate_IdOrderByIdDesc(target.getId(), reportingDate.getId());
        } else {
            evidenceList = repository.findEvidenceByTargetAndReportingDateOrderByIdDesc(target, reportingDate);
        }
        if (evidenceList == null || evidenceList.isEmpty()) {
            return null;
        }
        return evidenceList.get(0);
    }

}
