package hr.performancemanagement.entities;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String status;
    private String message;
    @ManyToOne
    @JoinColumn(name = "scorecard_id")
    private Scorecard scorecard;
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
    @CreationTimestamp
    private Date date;

    public Approval() {
    }

    public Approval(long id, String status, String message, Scorecard scorecard, Account account, Date date) {
        this.id = id;
        this.status = status;
        this.message = message;
        this.scorecard = scorecard;
        this.account = account;
        this.date = date;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Scorecard getScorecard() {
        return scorecard;
    }

    public void setScorecard(Scorecard scorecard) {
        this.scorecard = scorecard;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
