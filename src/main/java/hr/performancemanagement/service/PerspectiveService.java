package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Department;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.Perspective;
import hr.performancemanagement.repository.GoalRepository;
import hr.performancemanagement.repository.PerspectiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PerspectiveService {

    @Autowired
    PerspectiveRepository perspectiveRepository;

    public Perspective getPerspectiveById(long id){

        Perspective perspective = perspectiveRepository.findPerspectiveById(id);
        return perspective;
    }

    public List<Perspective> listAllPerspectives(long clientId)
    {
        List<Perspective> perspectiveList = new ArrayList<>();
        perspectiveRepository.findPerspectivesByClientId(clientId).forEach(perspective -> perspectiveList.add(perspective));
        return perspectiveList;
    }

    public void addPerspective(Perspective perspective) {

        perspectiveRepository.save(perspective);
    }

}
