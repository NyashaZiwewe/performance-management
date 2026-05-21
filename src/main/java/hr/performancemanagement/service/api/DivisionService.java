package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Division;
import java.util.List;

public interface DivisionService {

    List<Division> listAllDivisions();

    List<Division> listAllDivisions(long clientId);

    Division getDivisionById(long id);

    void addDivision(Division division);

    Division saveDivision(Division division);

    void deleteDivision(Division division);
}
