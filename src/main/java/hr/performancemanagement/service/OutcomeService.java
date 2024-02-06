package hr.performancemanagement.service;
import hr.performancemanagement.entities.Outcome;
import hr.performancemanagement.repository.OutcomeRepository;
import hr.performancemanagement.repository.TargetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OutcomeService {
    @Autowired
    OutcomeRepository outcomeRepository;
    @Autowired
    private TargetRepository targetRepository;

    public Outcome getOutcomeById(long id){

        Outcome outcome = outcomeRepository.findOutcomeById(id);
        return outcome;
    }
    public List<Outcome> listAllOutcomes(long scorecardId){
        List<Outcome> outcomes = new ArrayList<>();
        outcomeRepository.findOutcomesByGoal(scorecardId).forEach(goal -> outcomes.add(goal));
        return outcomes;
    }


    public Outcome saveOutcome(Outcome outcome) {

        Outcome savedOutcome = outcomeRepository.save(outcome);
       return savedOutcome;
    }

    public double getTotalAllocatedWeight(long scorecardId){
        try {
            double total = outcomeRepository.sumAllocatedWeight(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    public double getAverageManagerScore(long scorecardId){
        try {
            double total = outcomeRepository.averageManagerScore(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    public double getAverageEmployeeScore(long scorecardId){
        try {
            double total = outcomeRepository.averageEmployeeScore(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    public double getAverageModeratorScore(long scorecardId){
        try {
            double total = outcomeRepository.averageModeratedScore(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    public double getAverageAgreedScore(long scorecardId){
        try {
            double total = outcomeRepository.averageModeratedScore(scorecardId);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    @Transactional
    public void deleteOutcome(Outcome outcome){
        outcomeRepository.delete(outcome);
    }

}
