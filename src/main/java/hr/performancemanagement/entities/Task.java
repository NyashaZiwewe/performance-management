package hr.performancemanagement.entities;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Date;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "action_plan_id")
    private ActionPlan actionPlan;
    @ManyToOne
    @JoinColumn(name = "assigned_id")
    private Account assigned;
    private String name;
    private String status;
    private Date dueDate;
    @UpdateTimestamp
    private Date lastUpdated;
    @CreationTimestamp
    @Column(updatable = false)
    private Date date;

    public Task() {
    }

    public Task(long id, ActionPlan actionPlan, Account assigned, String name, String status, Date dueDate, Date lastUpdated, Date date) {
        this.id = id;
        this.actionPlan = actionPlan;
        this.assigned = assigned;
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

    public ActionPlan getActionPlan() {
        return actionPlan;
    }

    public void setActionPlan(ActionPlan actionPlan) {
        this.actionPlan = actionPlan;
    }

    public Account getAssigned() {
        return assigned;
    }

    public void setAssigned(Account assigned) {
        this.assigned = assigned;
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
