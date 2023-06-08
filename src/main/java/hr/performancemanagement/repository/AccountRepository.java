package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findAccountsByClientId(long id);
    List<Account> findAccountsByAccountType(String accountType);
    List<Account> findAccountsBySupervisor(Account account);
    List<Account> findAccountsByDepartment(Department department);
//    List<Account> findAccountsByAccountsEqualsIgnoreCase(String accountType);
//    List<Account> findAccountsByAccountTypeNotContainingIgnoreCase(String accountType);
    Account findAccountById(long id);

    public Account findAccountByEmail(String email);

    public Account findAccountByEmailAndPassword(String username,String password);

}
