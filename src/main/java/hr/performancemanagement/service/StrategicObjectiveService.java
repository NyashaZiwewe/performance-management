package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Perspective;
import hr.performancemanagement.entities.StrategicObjective;
import hr.performancemanagement.repository.ReportingPeriodRepository;
import hr.performancemanagement.repository.StrategicObjectiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StrategicObjectiveService {

    @Autowired
    StrategicObjectiveRepository strategicObjectiveRepository;

    public StrategicObjective getStrategicObjectiveById(long id){

        StrategicObjective strategicObjective = strategicObjectiveRepository.findStrategicObjectiveById(id);
        return strategicObjective;
    }
    public List<StrategicObjective> listAllStrategicObjectives(long reportingPeriodId)
    {
        List<StrategicObjective> strategicObjectiveList = new ArrayList<>();
        strategicObjectiveRepository.findStrategicObjectivesByReportingPeriodId(reportingPeriodId).forEach(strategicObjective -> strategicObjectiveList.add(strategicObjective));
        return strategicObjectiveList;
    }

    public List<StrategicObjective> listStrategicObjectivesByScorecard(long scorecardId)
    {
        List<StrategicObjective> strategicObjectiveList = new ArrayList<>();
        strategicObjectiveRepository.strategicObjectivesByScorecard(scorecardId).forEach(strategicObjective -> strategicObjectiveList.add(strategicObjective));
        for(StrategicObjective objective : strategicObjectiveList){
            objective.setWeightedScore(strategicObjectiveRepository.weightedScoreByScorecardAndStrategicObjective(objective));
        }
        return strategicObjectiveList;
    }

    public void addStrategicObjective(StrategicObjective strategicObjective) {

        strategicObjectiveRepository.save(strategicObjective);
    }

    public StrategicObjective saveStrategicObjective(StrategicObjective strategicObjective){
        StrategicObjective savedStrategicObjective = strategicObjectiveRepository.save(strategicObjective);
        return savedStrategicObjective;
    }
}
