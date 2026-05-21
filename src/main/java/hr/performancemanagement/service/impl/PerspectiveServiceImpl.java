package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Department;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.Perspective;
import hr.performancemanagement.repository.GoalRepository;
import hr.performancemanagement.repository.PerspectiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class PerspectiveServiceImpl implements hr.performancemanagement.service.api.PerspectiveService {

    @Autowired
    PerspectiveRepository perspectiveRepository;

    @Override
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

    @Override
    public void addPerspective(Perspective perspective) {

        perspectiveRepository.save(perspective);
    }

    @Override
    public Perspective savePerspective(Perspective perspective) {
        return perspectiveRepository.save(perspective);
    }

    @Transactional
    @Override
    public void deletePerspective(Perspective perspective) {
        perspectiveRepository.delete(perspective);
    }

}
