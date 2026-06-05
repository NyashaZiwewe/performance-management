package hr.performancemanagement.service;

import hr.performancemanagement.entities.OverallComment;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.OverallCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OverallCommentService {

    @Autowired
    private OverallCommentRepository overallCommentRepository;

    public OverallComment saveOverallComment(OverallComment overallComment) {
        boolean exists = overallCommentRepository.existsOverallCommentByScorecardAndAndReportingDate(overallComment.getScorecard(),overallComment.getReportingDate());
        OverallComment comment;
        if (exists) {
            comment = overallCommentRepository.findOverallCommentByScorecardAndReportingDate(overallComment.getScorecard(),overallComment.getReportingDate());
            if(overallComment.getOwnerComment() != null){
                comment.setOwnerComment(overallComment.getOwnerComment());
            }
            if(overallComment.getSupervisorComment() != null){
                comment.setSupervisorComment(overallComment.getSupervisorComment());
            }
            if(overallComment.getModeratorComment() != null){
                comment.setModeratorComment(overallComment.getModeratorComment());
            }
           comment = overallCommentRepository.save(comment);
        }else {
           comment = overallCommentRepository.save(overallComment);
        }
        return comment;
    }

    public List<OverallComment> getOverallCommentsByScorecard(Scorecard scorecard) {
        List<OverallComment> comments = new ArrayList<>();
        comments = overallCommentRepository.findOverallCommentsByScorecard(scorecard);
        return comments;
    }
}
