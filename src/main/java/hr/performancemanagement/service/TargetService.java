package hr.performancemanagement.service;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.OutcomeRepository;
import hr.performancemanagement.repository.OutputRepository;
import hr.performancemanagement.repository.TargetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TargetService {
    @Autowired
    TargetRepository targetRepository;
    @Autowired
    OutputRepository outputRepository;
    @Autowired
    ReportingDateService reportingDateService;

    public List<Target> getAllTargetsByScorecard(Scorecard scorecard){
        List<Target> targetList = new ArrayList<>();

        List<Output> outputs = outputRepository.findOutputsByScorecard(scorecard);

        for(Output output: outputs){
//           List<Target> targets = targetRepository.findTargetsByOutput(output);
            for(Target target: output.getTargets()){
                target.setOutcome(output.getOutcome());
                target.setGoal(output.getOutcome().getGoal());
                target.setGear(output.getOutcome().getGoal().getGear());
                targetList.add(target);
            }
        }

        return targetList;
    }

    public List<Target> getAllTargetsByOutput(Output output){
        List<Target> targets = targetRepository.findTargetsByOutput(output);
        return targets;
    }
    public Target getTargetById(long id){
        Target target = targetRepository.findTargetById(id);
        return target;
    }

    public boolean checkIfOutputHasTargets(Output output){
        int count = targetRepository.countTargetsByOutput(output);
        if(count < 1){
            return false;
        }else {
            return true;
        }
    }

//    public boolean  updateWeightedTargetScore(Target target){
//
//        double sumActual = targetRepository.totalWeightedScoreByTarget(target);
//        target.setWeightedScore(sumActual);
//        try {
//            saveTarget(target);
//            return true;
//        }catch (Exception e){
//            return false;
//        }
//    }
//
//    public boolean  updateActualTargetScore(Target target){
//
//        double avg = targetRepository.averageActualScoreByTarget(target);
//        target.setActualScore(avg);
//        try {
//            saveTarget(target);
//            return true;
//        }catch (Exception e){
//            return false;
//        }
//    }
//
//    public boolean  updateManagerTargetScore(Target target){
//
//        double avg = targetRepository.averageManagerScoreByTarget(target);
//        target.setManagerScore(avg);
//        try {
//            saveTarget(target);
//            return true;
//        }catch (Exception e){
//            return false;
//        }
//    }
//
//    public boolean  updateEmployeeTargetScore(Target target){
//
//        double avg = targetRepository.averageEmployeeScoreByTarget(target);
//        target.setEmployeeScore(avg);
//        try {
//            saveTarget(target);
//            return true;
//        }catch (Exception e){
//            return false;
//        }
//    }


    public Target saveTarget(Target target) {
        Target savedTarget = targetRepository.save(target);
        return savedTarget;
    }

    @Transactional
    public void deleteTarget(Target target){
       try {
           targetRepository.delete(target);
       }catch (Exception e){
           System.out.println(e.getMessage());
       }
    }
}
