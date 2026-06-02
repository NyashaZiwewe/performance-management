package hr.performancemanagement.service;

import hr.performancemanagement.entities.Evidence;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Target;
import hr.performancemanagement.repository.EvidenceRepository;
import hr.performancemanagement.repository.TargetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvidenceService {

    @Autowired
    EvidenceRepository repository;
    @Autowired
    private TargetRepository targetRepository;

    public boolean existsEvidenceByTargetAndReportingDate(Target target, ReportingDate reportingDate) {
        return repository.existsEvidenceByTargetAndReportingDate(target, reportingDate);
    }

    public void saveEvidence(Evidence evidence) {
        Evidence evy;
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
            evy = repository.save(existingEvidence);
        }else {
           evy = repository.save(evidence);
        }
        Target target = evy.getTarget();
        target.setCurrentEvidence(evy.getEvidence());
        target.setCurrentJustification(evy.getJustification());
        target.setCurrentAttachmentName(evy.getAttachmentName());
        targetRepository.save(target);
    }

    private Evidence getLatestEvidence(Target target, ReportingDate reportingDate) {
        List<Evidence> evidenceList = repository.findEvidenceByTargetAndReportingDateOrderByIdDesc(target, reportingDate);
        if (evidenceList == null || evidenceList.isEmpty()) {
            return null;
        }
        return evidenceList.get(0);
    }

}
