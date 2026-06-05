package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.StrategicObjective;
import hr.performancemanagement.repository.StrategicObjectiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class StrategicObjectiveServiceImpl implements hr.performancemanagement.service.api.StrategicObjectiveService {

    @Autowired
    StrategicObjectiveRepository strategicObjectiveRepository;

    @Override
    public StrategicObjective getStrategicObjectiveById(long id){

        StrategicObjective strategicObjective = strategicObjectiveRepository.findStrategicObjectiveById(id);
        return strategicObjective;
    }
    @Override
    public List<StrategicObjective> listAllStrategicObjectives(long reportingPeriodId)
    {
        List<StrategicObjective> strategicObjectiveList = new ArrayList<>();
        strategicObjectiveRepository.findStrategicObjectivesByReportingPeriodId(reportingPeriodId).forEach(strategicObjective -> strategicObjectiveList.add(strategicObjective));
        return strategicObjectiveList;
    }

    @Override
    public long countStrategicObjectives(long reportingPeriodId) {
        return strategicObjectiveRepository.countByReportingPeriodId(reportingPeriodId);
    }

    @Override
    public void addStrategicObjective(StrategicObjective strategicObjective) {

        strategicObjectiveRepository.save(strategicObjective);
    }

    @Override
    public StrategicObjective saveStrategicObjective(StrategicObjective strategicObjective){
        StrategicObjective savedStrategicObjective = strategicObjectiveRepository.save(strategicObjective);
        return savedStrategicObjective;
    }

    @Transactional
    @Override
    public void deleteStrategicObjective(StrategicObjective strategicObjective) {
        strategicObjectiveRepository.delete(strategicObjective);
    }
}
