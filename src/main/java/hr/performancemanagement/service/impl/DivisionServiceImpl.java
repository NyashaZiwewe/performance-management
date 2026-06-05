package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Division;
import hr.performancemanagement.exception.ResourceNotFoundException;
import hr.performancemanagement.repository.DivisionRepository;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.DivisionService;
import hr.performancemanagement.utils.constants.Client;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DivisionServiceImpl implements DivisionService {

    private final DivisionRepository divisionRepository;
    private final CommonService commonService;

    public DivisionServiceImpl(DivisionRepository divisionRepository, CommonService commonService) {
        this.divisionRepository = divisionRepository;
        this.commonService = commonService;
    }

    @Override
    public List<Division> listAllDivisions() {
        Account loggedUser = commonService.getLoggedUser();
        long clientId = loggedUser != null ? loggedUser.getClientId() : Client.CLIENT_ID;
        return listAllDivisions(clientId);
    }

    @Override
    public List<Division> listAllDivisions(long clientId) {
        List<Division> divisions = new ArrayList<Division>();
        divisions.addAll(divisionRepository.findDivisionsByClientId(clientId));
        return divisions;
    }

    @Override
    public Division getDivisionById(long id) {
        Division division = divisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Division not found with id " + id));
        return division;
    }

    @Override
    public Division saveDivision(Division division) {
        if (division == null) {
            throw new IllegalArgumentException("Division cannot be null");
        }
        return divisionRepository.save(division);
    }

    @Override
    @Transactional
    public void deleteDivision(Division division) {
        if (division == null) {
            throw new IllegalArgumentException("Division cannot be null");
        }
        divisionRepository.delete(division);
    }
}
