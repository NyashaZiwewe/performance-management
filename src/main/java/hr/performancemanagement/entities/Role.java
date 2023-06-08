//package hr.performancemanagement.entities;
//
//import javax.persistence.*;
//import java.util.List;
//
//@Entity
//public class Role {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//    @Column(nullable = false, unique = true)
//    private String name;
//
////    private List<Account> accounts;
//
//    public Role() {
//    }
//
//    public Role(Long id, String name, List<Account> accounts) {
//        this.id = id;
//        this.name = name;
//        this.accounts = accounts;
//    }
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public List<Account> getAccounts() {
//        return accounts;
//    }
//
//    public void setAccounts(List<Account> accounts) {
//        this.accounts = accounts;
//    }
//}
