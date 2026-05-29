package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Department;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;


@Service
public class AccountServiceImpl implements hr.performancemanagement.service.api.AccountService {

    @Autowired
    AccountRepository accountRepository;
    @Autowired
    CommonService cs;

    @Override
    public List<Account> listAllAccounts(){
        List<Account> accountList = new ArrayList<>();

        Account loggedUser = cs.getLoggedUser();

        if(cs.isAdmin() || cs.hasSpecialRights()){
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

    @Override
    public List<Account> listAllAccountsByClientId(long clientId) {
        List<Account> accountList = new ArrayList<>();
        accountRepository.findAccountsByClientId(clientId).forEach(accountList::add);
        return accountList;
    }

    @Override
    public Account getAccountById(long id){

       Account account = accountRepository.findAccountById(id);
       return account;
    }

   @Override
   public Account getAccountToReset(String reset){

        Account account = accountRepository.findAccountByResetPassword(cs.hashResetToken(reset));
        return account;
    }

   @Override
   public Account getAccountToReset(String email, String reset){

        Account account = accountRepository.findAccountByEmailAndResetPassword(email, cs.hashResetToken(reset));
        return account;
    }

   @Override
   @Transactional
   public void addAccount(Account account) {
        if (account != null && account.getPassword() != null && cs.requiresPasswordUpgrade(account.getPassword())) {
            account.setPassword(cs.encodePassword(account.getPassword()));
        }

        accountRepository.save(account);
   }

   @Override
   @Transactional
   public Account saveAccount(Account account){
        if (account != null && account.getPassword() != null && cs.requiresPasswordUpgrade(account.getPassword())) {
            account.setPassword(cs.encodePassword(account.getPassword()));
        }
        Account savedAccount = accountRepository.save(account);
        return savedAccount;
    }

    @Override
    @Transactional
    public Account updatePasswordResetToken(Account account, String resetTokenHash) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        account.setResetPassword(resetTokenHash);
        return accountRepository.save(account);
    }

    @Override
    @Transactional
    public Account updatePasswordFromReset(Account account, String encodedPassword) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        account.setPassword(encodedPassword);
        account.setResetPassword(null);
        return accountRepository.save(account);
    }

    @Override
    public Account findAccountByEmail(String email) {
        return accountRepository.findAccountByEmail(email);
    }

    @Transactional
    @Override
    public void deleteAccount(Account account){
        accountRepository.delete(account);
    }

    @Transactional
    @Override
    public void upgradePassword(long accountId, String encodedPassword) {
        accountRepository.updatePasswordById(accountId, encodedPassword);
    }



}
