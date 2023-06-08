package hr.performancemanagement.entities;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Date;

@Entity
public class PIPTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "performance_improvement_plan_id")
    private PerformanceImprovementPlan performanceImprovementPlan;
    private String name;
    private String status;
    private Date dueDate;
    @UpdateTimestamp
    private Date lastUpdated;
    @CreationTimestamp
    @Column(updatable = false)
    private Date date;

    public PIPTask() {
    }

    public PIPTask(long id, PerformanceImprovementPlan performanceImprovementPlan, Account assigned, String name, String status, Date dueDate, Date lastUpdated, Date date) {
        this.id = id;
        this.performanceImprovementPlan = performanceImprovementPlan;
        this.name = name;
        this.status = status;
        this.dueDate = dueDate;
        this.lastUpdated = lastUpdated;
        this.date = date;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
