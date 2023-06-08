package hr.performancemanagement.service;

import hr.performancemanagement.entities.Department;
import hr.performancemanagement.entities.Division;
import hr.performancemanagement.repository.DivisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Service
public class DivisionService {

    @Autowired
    DivisionRepository divisionRepository;

    @Autowired
    HttpSession session;

    public List<Division> listAllDivisions()
    {
        Long clientId = (Long) session.getAttribute("clientId");
        List<Division> divisionsList = new ArrayList<>();
        divisionRepository.findDivisionsByClientId(clientId).forEach(division -> divisionsList.add(division));
        return divisionsList;
    }

    public void addDivision(Division division) {
        divisionRepository.save(division);
    }
}
