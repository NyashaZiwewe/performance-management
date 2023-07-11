package hr.performancemanagement.service.ScoreService;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.GoalRepository;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.service.TargetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValueBasedScoreService {
    @Autowired
    ScoreRepository scoreRepository;

    @Autowired
    private TargetService targetService;

    public Score getScoreById(long id){

        Score score = scoreRepository.findScoreById(id);
        return score;
    }

//   public double getAverageEmployeeScore(long scorecardId){
//       double score = scoreRepository.averageEmployeeScore(scorecardId);
//       return score;
//   }
//
//    public double getAverageManagerScore(long scorecardId){
//        double score = scoreRepository.averageManagerScore(scorecardId);
//        return score;
//    }
//    public double getAverageActualScore(long scorecardId){
//        double score = scoreRepository.averageActualScore(scorecardId);
//        return score;
//    }
//
//    public double getAverageEmployeeScoreForTarget(Target target){
//        double score = scoreRepository.averageEmployeeScoreForTarget(target);
//        return score;
//    }
//
//    public double getAverageManagerScoreForTarget(Target target){
//        double score = scoreRepository.averageManagerScoreForTarget(target);
//        return score;
//    }
//    public double getAverageActualScoreForTarget(Target target){
//        double score = scoreRepository.averageActualScoreForTarget(target);
//        return score;
//    }

    public boolean  updateTargetData(Target target){

        double weightedRating = scoreRepository.totalWeightedScoreByTarget(target);
        double actualScore = scoreRepository.averageActualScoreByTarget(target);
        double managerScore = scoreRepository.averageManagerScoreByTarget(target);
        double employeeScore = scoreRepository.averageEmployeeScoreByTarget(target);

        target.setWeightedScore(weightedRating);
        target.setActualScore(actualScore);
        target.setManagerScore(managerScore);
        target.setEmployeeScore(employeeScore);

        try {
            targetService.saveTarget(target);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public void saveScore(Score score) {

        scoreRepository.save(score);
        updateTargetData(score.getTarget());
    }

    @Transactional
    public void deleteScore(Score score){
        scoreRepository.delete(score);
    }
}
