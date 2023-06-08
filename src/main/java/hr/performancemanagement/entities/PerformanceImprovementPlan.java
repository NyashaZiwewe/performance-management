package hr.performancemanagement.entities;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Date;
//import java.util.Date;

@Entity
public class PerformanceImprovementPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Account employee;
    @ManyToOne
    @JoinColumn(name = "reporting_period_id")
    private ReportingPeriod reportingPeriod;

    private String targetArea;
    private String concern;
    private String expectedStandard;
    private String agreedAction;
    private String requiredSupport;
    private String reviewNotes;
    private double progress;
    private String status;
    private String endDate;
    @CreationTimestamp
    @Column(updatable = false)
    private Date date;



    public PerformanceImprovementPlan() {
    }

    public PerformanceImprovementPlan(long id, long clientId, Account employee, ReportingPeriod reportingPeriod, String targetArea, String concern, String expectedStandard, String agreedAction, String requiredSupport, String reviewNotes, double progress, String status, String endDate, Date date) {
        this.id = id;
        this.clientId = clientId;
        this.employee = employee;
        this.reportingPeriod = reportingPeriod;
        this.targetArea = targetArea;
        this.concern = concern;
        this.expectedStandard = expectedStandard;
        this.agreedAction = agreedAction;
        this.requiredSupport = requiredSupport;
        this.reviewNotes = reviewNotes;
        this.progress = progress;
        this.status = status;
        this.endDate = endDate;
        this.date = date;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public Account getEmployee() {
        return employee;
    }

    public void setEmployee(Account employee) {
        this.employee = employee;
    }

    public ReportingPeriod getReportingPeriod() {
        return reportingPeriod;
    }

    public void setReportingPeriod(ReportingPeriod reportingPeriod) {
        this.reportingPeriod = reportingPeriod;
    }

    public String getTargetArea() {
        return targetArea;
    }

    public void setTargetArea(String targetArea) {
        this.targetArea = targetArea;
    }

    public String getConcern() {
        return concern;
    }

    public void setConcern(String concern) {
        this.concern = concern;
    }

    public String getExpectedStandard() {
        return expectedStandard;
    }

    public void setExpectedStandard(String expectedStandard) {
        this.expectedStandard = expectedStandard;
    }

    public String getAgreedAction() {
        return agreedAction;
    }

    public void setAgreedAction(String agreedAction) {
        this.agreedAction = agreedAction;
    }

    public String getRequiredSupport() {
        return requiredSupport;
    }

    public void setRequiredSupport(String requiredSupport) {
        this.requiredSupport = requiredSupport;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
