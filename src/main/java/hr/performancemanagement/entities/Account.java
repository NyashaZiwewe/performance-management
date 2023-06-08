package hr.performancemanagement.entities;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    private String fullName;
    @Column(unique=true)
    private String email;
    private String password;
    @Nullable
    private String resetPassword;

//    @ManyToMany(fetch = FetchType.EAGER, cascade=CascadeType.ALL)
//    @JoinTable(
//            name="ACCOUNTS_ROLES",
//            joinColumns={@JoinColumn(name="ACCOUNT_ID", referencedColumnName="ID")},
//            inverseJoinColumns={@JoinColumn(name="ROLE_ID", referencedColumnName="ID")})
//    private List<Role> roles = new ArrayList<>();
    private String role;
    @ManyToOne
    @JoinColumn(name = "supervisor_id", referencedColumnName = "id")
    private Account supervisor;
    @ManyToOne
    @JoinColumn(name = "department_id", referencedColumnName = "id")
    private Department department;
    private String position;
    private String accountType;
    @ManyToOne
    @JoinColumn(name = "division_id", referencedColumnName = "id")
    private Division division;
    private String special;
    private String admin;
    private String accounts;
    private String status;
    @CreationTimestamp()
    private Date date;

    public Account(long id, long clientId, String fullName, String email, String password, String resetPassword, String role, Account supervisor, Department department, String position, String accountType, Division division, String special, String admin, String accounts, String status, Date date) {
        this.id = id;
        this.clientId = clientId;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.resetPassword = resetPassword;
        this.role = role;
        this.supervisor = supervisor;
        this.department = department;
        this.position = position;
        this.accountType = accountType;
        this.division = division;
        this.special = special;
        this.admin = admin;
        this.accounts = accounts;
        this.status = status;
        this.date = date;
    }


    public Account() {

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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getResetPassword() {
        return resetPassword;
    }

    public void setResetPassword(String resetPassword) {
        this.resetPassword = resetPassword;
    }

    public Account getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Account supervisor) {
        this.supervisor = supervisor;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Division getDivision() {
        return division;
    }

    public void setDivision(Division division) {
        this.division = division;
    }

    public String getSpecial() {
        return special;
    }

    public void setSpecial(String special) {
        this.special = special;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public String getAccounts() {
        return accounts;
    }

    public void setAccounts(String accounts) {
        this.accounts = accounts;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
