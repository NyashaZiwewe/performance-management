package hr.performancemanagement.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long clientId;
    private String client;
    private String profile;
    private boolean isMandatory;

    public Client(long clientId, String client, String profile, boolean isMandatory) {
        this.clientId = clientId;
        this.client = client;
        this.profile = profile;
        this.isMandatory = isMandatory;
    }

    public Client() {
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public boolean getIsMandatory(){return  isMandatory; }

    public void setMandatory(boolean isMandatory){this.isMandatory = isMandatory;}
}
