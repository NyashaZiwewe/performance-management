package hr.performancemanagement.entities;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Date;


@Entity
public class Scorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Account owner;
    @ManyToOne
    @JoinColumn(name = "reporting_period_id")
    private ReportingPeriod reportingPeriod;
    private String status;
    @Transient
    private double employeeScore;
    @Transient
    private double managerScore;
    @Transient
    private double actualScore;
    @Column(columnDefinition = "varchar(50) default 'NEW'")
    private String approvalStatus;
    @UpdateTimestamp
    private Date lastUpdate;
    @CreationTimestamp
    private Date date;
    private String lockStatus;

    public Scorecard() {
    }

    public Scorecard(long id, long clientId, Account owner, ReportingPeriod reportingPeriod, String status, double employeeScore, double managerScore, double actualScore, String approvalStatus, Date lastUpdate, Date date, String lockStatus) {
        this.id = id;
        this.clientId = clientId;
        this.owner = owner;
        this.reportingPeriod = reportingPeriod;
        this.status = status;
        this.employeeScore = employeeScore;
        this.managerScore = managerScore;
        this.actualScore = actualScore;
        this.approvalStatus = approvalStatus;
        this.lastUpdate = lastUpdate;
        this.date = date;
        this.lockStatus = lockStatus;
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

    public Account getOwner() {
        return owner;
    }

    public void setOwner(Account owner) {
        this.owner = owner;
    }

    public ReportingPeriod getReportingPeriod() {
        return reportingPeriod;
    }

    public void setReportingPeriod(ReportingPeriod reportingPeriod) {
        this.reportingPeriod = reportingPeriod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getEmployeeScore() {
        return employeeScore;
    }

    public void setEmployeeScore(double employeeScore) {
        this.employeeScore = employeeScore;
    }

    public double getManagerScore() {
        return managerScore;
    }

    public void setManagerScore(double managerScore) {
        this.managerScore = managerScore;
    }

    public double getActualScore() {
        return actualScore;
    }

    public void setActualScore(double actualScore) {
        this.actualScore = actualScore;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getLockStatus() {
        return lockStatus;
    }

    public void setLockStatus(String lockStatus) {
        this.lockStatus = lockStatus;
    }
}
