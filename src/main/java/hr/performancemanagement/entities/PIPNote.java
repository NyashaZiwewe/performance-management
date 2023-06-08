package hr.performancemanagement.entities;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
public class PIPNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "performance_improvement_plan_id")
    private PerformanceImprovementPlan performanceImprovementPlan;
    private String comment;
    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Account employee;
    private String status;
    @CreationTimestamp
    private Date date;

    public PIPNote(long id, PerformanceImprovementPlan performanceImprovementPlan, String comment, Account employee, String status, Date date) {
        this.id = id;
        this.performanceImprovementPlan = performanceImprovementPlan;
        this.comment = comment;
        this.employee = employee;
        this.status = status;
        this.date = date;
    }

    public PIPNote() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public PerformanceImprovementPlan getPerformanceImprovementPlan() {
        return performanceImprovementPlan;
    }

    public void setPerformanceImprovementPlan(PerformanceImprovementPlan performanceImprovementPlan) {
        this.performanceImprovementPlan = performanceImprovementPlan;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Account getEmployee() {
        return employee;
    }

    public void setEmployee(Account employee) {
        this.employee = employee;
    }

}
