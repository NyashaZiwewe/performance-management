package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Department;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountService {

    @Autowired
    AccountRepository accountRepository;
    @Autowired
    HttpSession session;

    public List<Account> listAllAccounts(){
        List<Account> accountList = new ArrayList<>();

        Account loggedUser = (Account) session.getAttribute("loggedUser");

        if(loggedUser.getAdmin().equalsIgnoreCase("IS_ADMIN") || loggedUser.getSpecial().equalsIgnoreCase("HAS_SPECIAL_RIGHTS")){
            accountRepository.findAccountsByClientId(loggedUser.getClientId()).forEach(account -> accountList.add(account));
        }
        else if(loggedUser.getAccountType().equalsIgnoreCase("Employee")){

            accountList.add(loggedUser);

        }else if(loggedUser.getAccountType().equalsIgnoreCase("Supervisor")){

            accountList.add(loggedUser);
            accountRepository.findAccountsBySupervisor(loggedUser).forEach(account -> accountList.add(account));

        }else if(loggedUser.getAccountType().equalsIgnoreCase("DEPARTMENT_MANAGER") || loggedUser.getAccountType().equalsIgnoreCase("DIVISIONAL_DIRECTOR")){

            accountRepository.findAccountsByDepartment(loggedUser.getDepartment()).forEach(account -> accountList.add(account));

        }else if(loggedUser.getAccountType().equalsIgnoreCase("ACTING_CEO") || loggedUser.getAccountType().equalsIgnoreCase("CEO") ){

            accountRepository.findAccountsByClientId(loggedUser.getClientId()).forEach(account -> accountList.add(account));

        }else{

        }

        return accountList;
    }

    public Account getAccountById(long id){

       Account account = accountRepository.findAccountById(id);
       return account;
    }

   public void addAccount(Account account) {

        accountRepository.save(account);
   }

   public Account saveAccount(Account account){
        Account savedAccount = accountRepository.save(account);
        return savedAccount;
    }

    public Account findAccountByEmail(String email) {
        return accountRepository.findAccountByEmail(email);
    }

    @Transactional
    public void deleteAccount(Account account){
        accountRepository.delete(account);
    }



}
