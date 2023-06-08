package hr.performancemanagement.entities;

import javax.persistence.*;

@Entity
public class Division {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     private long id;
     private String name;
    @Column(updatable = false)
     private long clientId;
     @Column(unique = true)
     private String code;
     @Column(unique = true)
     private String address;
     private String email;
     private String phone;
     private String website;
     @Column(unique = true)
     private String colorCode;

    public Division(long id, String name, long clientId, String code, String address, String email, String phone, String website, String colorCode) {
        this.id = id;
        this.name = name;
        this.clientId = clientId;
        this.code = code;
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.website = website;
        this.colorCode = colorCode;
    }

    public Division() {
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }
}
