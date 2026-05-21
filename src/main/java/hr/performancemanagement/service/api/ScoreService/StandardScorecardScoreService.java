package hr.performancemanagement.service.api.ScoreService;

import hr.performancemanagement.entities.Score;
import hr.performancemanagement.entities.Target;
import hr.performancemanagement.repository.ScoreRepository;

public interface StandardScorecardScoreService {
    double calculateWeightedScore(Score score);
    Score saveScore(Score score);
    boolean scoreExists(Score score);
    boolean  updateTargetData(Target target);
    void deleteScore(Score score);
}
