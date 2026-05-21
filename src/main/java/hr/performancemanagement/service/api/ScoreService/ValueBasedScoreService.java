package hr.performancemanagement.service.api.ScoreService;

import hr.performancemanagement.entities.Score;
import hr.performancemanagement.entities.Target;
import hr.performancemanagement.repository.ScoreRepository;
import java.util.function.BiConsumer;

public interface ValueBasedScoreService {
    double calculateWeightedScore(Score score);
    Score saveEmployeeScore(Score score);
    Score saveEvidence(Score score);
    Score saveManagerScore(Score score);
    Score saveAgreedScore(Score score);
    Score saveModeratedScore(Score score);
    boolean scoreExists(Score score);
    boolean  updateTargetData(Target target);
    void deleteScore(Score score);
}
