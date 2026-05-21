package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.ScorecardModel;
import hr.performancemanagement.repository.ScorecardModelRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import java.util.List;

public interface ScorecardModelService {
    List<ScorecardModel> getAllScorecardModels(long clientId);
    ScorecardModel getScorecardModelById(long id);
    ScorecardModel getActiveScorecardModel();
    boolean checkIfClientModelExits(long clientId, String name);
    void saveScorecardModel(ScorecardModel model);
    int deleteScorecardModel(ScorecardModel model);
}
