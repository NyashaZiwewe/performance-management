package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.repository.AccountRepository;
import hr.performancemanagement.repository.DepartmentRepository;
import hr.performancemanagement.repository.DivisionRepository;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.ConfigurationStatusService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConfigurationStatusServiceImpl implements ConfigurationStatusService {

    private final CommonService commonService;
    private final AccountRepository accountRepository;
    private final DivisionRepository divisionRepository;
    private final DepartmentRepository departmentRepository;

    public ConfigurationStatusServiceImpl(
            CommonService commonService,
            AccountRepository accountRepository,
            DivisionRepository divisionRepository,
            DepartmentRepository departmentRepository
    ) {
        this.commonService = commonService;
        this.accountRepository = accountRepository;
        this.divisionRepository = divisionRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public List<String> getAccountWarnings() {
        List<String> warnings = new ArrayList<String>();
        Account loggedUser = commonService.getLoggedUser();
        Long clientId = loggedUser != null ? loggedUser.getClientId() : null;

        if (clientId == null) {
            warnings.add("Set up system settings first before adding or editing accounts.");
            return warnings;
        }

        if (accountRepository.findAccountsByClientId(clientId).isEmpty()) {
            warnings.add("No accounts exist yet. Supervisor is optional for the first account and can be assigned later.");
        }
        if (divisionRepository.findDivisionsByClientId(clientId).isEmpty()) {
            warnings.add("No divisions found. Division can be left blank for now and updated later.");
        }
        if (departmentRepository.findDepartmentsByClientId(clientId).isEmpty()) {
            warnings.add("No departments found. Department can be left blank for now and updated later.");
        }
        return warnings;
    }
}
