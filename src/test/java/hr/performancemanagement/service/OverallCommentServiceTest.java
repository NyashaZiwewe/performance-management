package hr.performancemanagement.service;

import hr.performancemanagement.entities.OverallComment;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.OverallCommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverallCommentServiceTest {

    @Mock
    private OverallCommentRepository overallCommentRepository;

    private OverallCommentService service;

    @BeforeEach
    void setUp() {
        service = new OverallCommentService(overallCommentRepository);
    }

    @Test
    void mergesOneRoleCommentWithoutClearingExistingComments() {
        Scorecard scorecard = scorecard();
        ReportingDate reportingDate = reportingDate();
        OverallComment existing = new OverallComment();
        existing.setScorecard(scorecard);
        existing.setReportingDate(reportingDate);
        existing.setOwnerComment("Owner comment");
        existing.setSupervisorComment("Old supervisor comment");

        OverallComment incoming = new OverallComment();
        incoming.setScorecard(scorecard);
        incoming.setReportingDate(reportingDate);
        incoming.setSupervisorComment("Updated supervisor comment");

        when(overallCommentRepository.findOverallCommentByScorecardAndReportingDate(scorecard, reportingDate))
                .thenReturn(Optional.of(existing));
        when(overallCommentRepository.save(existing)).thenReturn(existing);

        OverallComment saved = service.saveOverallComment(incoming);

        assertSame(existing, saved);
        assertEquals("Owner comment", saved.getOwnerComment());
        assertEquals("Updated supervisor comment", saved.getSupervisorComment());
        assertNull(saved.getModeratorComment());
        verify(overallCommentRepository).save(existing);
    }

    @Test
    void returnsAnEmptyDatedCommentWhenNoneExists() {
        Scorecard scorecard = scorecard();
        ReportingDate reportingDate = reportingDate();
        when(overallCommentRepository.findOverallCommentByScorecardAndReportingDate(scorecard, reportingDate))
                .thenReturn(Optional.empty());

        OverallComment comment = service.getOverallCommentByScorecardAndReportingDate(scorecard, reportingDate);

        assertSame(scorecard, comment.getScorecard());
        assertSame(reportingDate, comment.getReportingDate());
        assertNull(comment.getOwnerComment());
    }

    @Test
    void rejectsCommentsWithoutAReportingDate() {
        OverallComment comment = new OverallComment();
        comment.setScorecard(scorecard());

        assertThrows(IllegalArgumentException.class, () -> service.saveOverallComment(comment));
    }

    private Scorecard scorecard() {
        Scorecard scorecard = new Scorecard();
        scorecard.setId(10L);
        return scorecard;
    }

    private ReportingDate reportingDate() {
        ReportingDate reportingDate = new ReportingDate();
        reportingDate.setId(20L);
        return reportingDate;
    }
}
