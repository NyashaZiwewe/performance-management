package hr.performancemanagement.entities;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.util.Date;

@Entity
public class ActionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    private String name;
    private String description;
    private String measureOfSuccess;
    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Account manager;
    @ManyToOne
    @JoinColumn(name = "reporting_period_id")
    private ReportingPeriod reportingPeriod;
    private String startDate;
    private String endDate;
    private double progress;
    private String status;
    @CreationTimestamp
    @Column(updatable = false)
    private Date date;

    public ActionPlan(long id, long clientId, String name, String description, String measureOfSuccess, Account manager, ReportingPeriod reportingPeriod, String startDate, String endDate, double progress, String status, Date date) {
        this.id = id;
        this.clientId = clientId;
        this.name = name;
        this.description = description;
        this.measureOfSuccess = measureOfSuccess;
        this.manager = manager;
        this.reportingPeriod = reportingPeriod;
        this.startDate = startDate;
        this.endDate = endDate;
        this.progress = progress;
        this.status = status;
        this.date = date;
    }

    public ActionPlan() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMeasureOfSuccess() {
        return measureOfSuccess;
    }

    public void setMeasureOfSuccess(String measureOfSuccess) {
        this.measureOfSuccess = measureOfSuccess;
    }

    public Account getManager() {
        return manager;
    }

    public void setManager(Account manager) {
        this.manager = manager;
    }
    public ReportingPeriod getReportingPeriod() {
        return reportingPeriod;
    }

    public void setReportingPeriod(ReportingPeriod reportingPeriod) {
        this.reportingPeriod = reportingPeriod;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
