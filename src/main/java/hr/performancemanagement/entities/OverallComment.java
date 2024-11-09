package hr.performancemanagement.entities;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
public class OverallComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "scorecard_id")
    private Scorecard scorecard;
    @ManyToOne
    @JoinColumn(name = "reporting_date_id")
    private ReportingDate reportingDate;
    private String ownerComment;
    private String supervisorComment;
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
