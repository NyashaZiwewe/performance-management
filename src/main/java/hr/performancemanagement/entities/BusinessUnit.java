package hr.performancemanagement.entities;

import javax.persistence.*;

@Entity
public class BusinessUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    @Column(updatable = false)
    private long clientId;
    private int head;

    public BusinessUnit(long id, String name, long clientId, int head) {
        this.id = id;
        this.name = name;
        this.clientId = clientId;
        this.head = head;
    }

    public BusinessUnit() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public int getHead() {
        return head;
    }

    public void setHead(int head) {
        this.head = head;
    }
}
