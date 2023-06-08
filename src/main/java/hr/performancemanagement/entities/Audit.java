package hr.performancemanagement.entities;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    private long userId;
    private String affectedTable;
    private String action;
    private String oldValue;
    private String newValue;
    @CreatedDate
    private Date date;

    public Audit(long id, long clientId, long userId, String affectedTable, String action, String oldValue, String newValue, Date date) {
        this.id = id;
        this.clientId = clientId;
        this.userId = userId;
        this.affectedTable = affectedTable;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.date = date;
    }


    public Audit() {

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

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getTable() {
        return affectedTable;
    }

    public void setTable(String table) {
        this.affectedTable = table;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
