package hr.performancemanagement.entities;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "action_plan_id")
    private ActionPlan actionPlan;
    private String comment;
    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Account employee;
    private String status;
    @CreationTimestamp
    private Date date;

    public ActionPlan getActionPlan() {
        return actionPlan;
    }

    public void setActionPlan(ActionPlan actionPlan) {
        this.actionPlan = actionPlan;
    }

    public Note(long id, ActionPlan actionPlan, String comment, Account employee, String status, Date date) {
        this.id = id;
        this.actionPlan = actionPlan;
        this.comment = comment;
        this.employee = employee;
        this.status = status;
        this.date = date;
    }

    public Note() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
