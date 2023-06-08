package hr.performancemanagement.entities;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Date;

@Entity
public class PIPIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "performance_improvement_plan_id")
    private PerformanceImprovementPlan performanceImprovementPlan;
    private String name;
    private String status;
    @CreationTimestamp
    @Column(updatable = false)
    private Date date;

    public PIPIssue(long id, PerformanceImprovementPlan performanceImprovementPlan, String name, String status, Date date) {
        this.id = id;
        this.performanceImprovementPlan = performanceImprovementPlan;
        this.name = name;
        this.status = status;
        this.date = date;
    }

    public PIPIssue() {
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
