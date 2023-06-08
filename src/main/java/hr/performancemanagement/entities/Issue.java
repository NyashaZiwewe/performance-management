package hr.performancemanagement.entities;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Date;

@Entity
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "action_plan_id")
    private ActionPlan actionPlan;
    private String name;
    private String status;
    @CreationTimestamp
    @Column(updatable = false)
    private Date date;

    public Issue(long id, ActionPlan actionPlan, String name, String status, Date date) {
        this.id = id;
        this.actionPlan = actionPlan;
        this.name = name;
        this.status = status;
        this.date = date;
    }

    public Issue() {
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
