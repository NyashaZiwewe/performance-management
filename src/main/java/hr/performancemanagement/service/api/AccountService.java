package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Department;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.repository.AccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.ArrayList;
import java.util.List;

public interface AccountService {
    List<Account> listAllAccounts();
    List<Account> listAllAccountsByClientId(long clientId);
    Account getAccountById(long id);
    Account getAccountToReset(String reset);
    Account getAccountToReset(String email, String reset);
    void addAccount(Account account);
    Account saveAccount(Account account);
    Account findAccountByEmail(String email);
    void deleteAccount(Account account);
    void upgradePassword(long accountId, String encodedPassword);
}
