package hr.performancemanagement.service;

import hr.performancemanagement.entities.OverallComment;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.OverallCommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OverallCommentService {

    private final OverallCommentRepository overallCommentRepository;

    public OverallCommentService(OverallCommentRepository overallCommentRepository) {
        this.overallCommentRepository = overallCommentRepository;
    }

    @Transactional
    public OverallComment saveOverallComment(OverallComment overallComment) {
        if (overallComment == null || overallComment.getScorecard() == null || overallComment.getReportingDate() == null) {
            throw new IllegalArgumentException("Scorecard and reporting date are required for an overall comment");
        }

        OverallComment persistedComment = overallCommentRepository
                .findOverallCommentByScorecardAndReportingDate(
                        overallComment.getScorecard(),
                        overallComment.getReportingDate()
                )
                .orElse(overallComment);
        mergeComments(persistedComment, overallComment);
        return overallCommentRepository.save(persistedComment);
    }

    public OverallComment getOverallCommentByScorecardAndReportingDate(Scorecard scorecard, ReportingDate reportingDate) {
        if (scorecard == null || reportingDate == null) {
            return emptyComment(scorecard, reportingDate);
        }
        return overallCommentRepository.findOverallCommentByScorecardAndReportingDate(scorecard, reportingDate)
                .orElseGet(() -> emptyComment(scorecard, reportingDate));
    }

    public List<OverallComment> getOverallCommentsByScorecard(Scorecard scorecard) {
        return overallCommentRepository.findOverallCommentsByScorecard(scorecard);
    }

    private void mergeComments(OverallComment persistedComment, OverallComment incomingComment) {
        if (incomingComment.getOwnerComment() != null) {
            persistedComment.setOwnerComment(incomingComment.getOwnerComment());
        }
        if (incomingComment.getSupervisorComment() != null) {
            persistedComment.setSupervisorComment(incomingComment.getSupervisorComment());
        }
        if (incomingComment.getModeratorComment() != null) {
            persistedComment.setModeratorComment(incomingComment.getModeratorComment());
        }
    }

    private OverallComment emptyComment(Scorecard scorecard, ReportingDate reportingDate) {
        OverallComment overallComment = new OverallComment();
        overallComment.setScorecard(scorecard);
        overallComment.setReportingDate(reportingDate);
        return overallComment;
    }
}
