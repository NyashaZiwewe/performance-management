package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.AccountAccessRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountAccessRoleRepository extends JpaRepository<AccountAccessRole, Long> {
    AccountAccessRole findAccountAccessRoleByIdAndClientId(long id, long clientId);
    List<AccountAccessRole> findAccountAccessRolesByAccountAndClientIdOrderByIdDesc(Account account, long clientId);
    List<AccountAccessRole> findAccountAccessRolesByClientIdOrderByIdDesc(long clientId);
}
