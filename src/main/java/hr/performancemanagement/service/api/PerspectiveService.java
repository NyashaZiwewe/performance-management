package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Department;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.Perspective;
import hr.performancemanagement.repository.GoalRepository;
import hr.performancemanagement.repository.PerspectiveRepository;
import java.util.ArrayList;
import java.util.List;

public interface PerspectiveService {
    Perspective getPerspectiveById(long id);
    List<Perspective> listAllPerspectives(long clientId);
    void addPerspective(Perspective perspective);
    Perspective savePerspective(Perspective perspective);
    void deletePerspective(Perspective perspective);
}
