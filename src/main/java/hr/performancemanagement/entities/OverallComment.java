package hr.performancemanagement.entities;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.Date;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uk_overall_comment_scorecard_reporting_date",
                columnNames = {"scorecard_id", "reporting_date_id"}
        )
)
public class OverallComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "scorecard_id", nullable = false)
    private Scorecard scorecard;
    @ManyToOne(optional = false)
    @JoinColumn(name = "reporting_date_id", nullable = false)
    private ReportingDate reportingDate;
    @Size(max = 1000, message = "Owner comment cannot exceed 1000 characters")
    @Column(length = 1000)
    private String ownerComment;
    @Size(max = 1000, message = "Supervisor comment cannot exceed 1000 characters")
    @Column(length = 1000)
    private String supervisorComment;
    @Size(max = 1000, message = "Moderator comment cannot exceed 1000 characters")
    @Column(length = 1000)
    private String moderatorComment;
    @CreationTimestamp
    private Date date;

    public Scorecard getScorecard() {
        return scorecard;
    }

    public void setScorecard(Scorecard scorecard) {
        this.scorecard = scorecard;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ReportingDate getReportingDate() {
        return reportingDate;
    }

    public void setReportingDate(ReportingDate reportingDate) {
        this.reportingDate = reportingDate;
    }

    public String getOwnerComment() {
        return ownerComment;
    }

    public void setOwnerComment(String ownerComment) {
        this.ownerComment = ownerComment;
    }

    public String getSupervisorComment() {
        return supervisorComment;
    }

    public void setSupervisorComment(String supervisorComment) {
        this.supervisorComment = supervisorComment;
    }

    public String getModeratorComment() {
        return moderatorComment;
    }

    public void setModeratorComment(String moderatorComment) {
        this.moderatorComment = moderatorComment;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
