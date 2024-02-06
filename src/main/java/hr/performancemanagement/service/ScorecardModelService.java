package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ScorecardModel;
import hr.performancemanagement.repository.ScorecardModelRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.List;

@Service
public class ScorecardModelService {
    @Autowired
    ScorecardModelRepository scorecardModelRepository;
    @Autowired
    CommonService cs;

    public List<ScorecardModel> getAllScorecardModels(long clientId){
        List<ScorecardModel> scorecardModelList = scorecardModelRepository.findAll();
        return scorecardModelList;
    }

    public ScorecardModel getScorecardModelById(long id){
        ScorecardModel model = scorecardModelRepository.findScorecardModelById(id);
        return model;
    }

    public ScorecardModel getActiveScorecardModel(){
        Account loggedUser = cs.getLoggedUser();
        ScorecardModel scorecardModel = scorecardModelRepository.findScorecardModelByClientIdAndStatus(loggedUser.getClientId(), PMConstants.STATUS_ACTIVE);
        return scorecardModel;
    }

    public boolean checkIfClientModelExits(long clientId, String name){

        int count = scorecardModelRepository.countScorecardModelsByClientIdAndName(clientId, name);
        if(count > 0){
            return true;
        }else {
            return false;
        }
    }

    public void saveScorecardModel(ScorecardModel model) {
        if(model.getStatus().equalsIgnoreCase(PMConstants.STATUS_ACTIVE)){
            Account loggedUser = cs.getLoggedUser();
            List<ScorecardModel> scorecardModelList = scorecardModelRepository.findScorecardModelsByClientId(loggedUser.getClientId());
            for(ScorecardModel scorecardModel: scorecardModelList){
                scorecardModel.setStatus(PMConstants.STATUS_IN_ACTIVE);
                scorecardModelRepository.save(scorecardModel);
            }
        }
        scorecardModelRepository.save(model);
    }

    @Transactional
    public int deleteScorecardModel(ScorecardModel model){
        Account loggedUser = cs.getLoggedUser();
        List<ScorecardModel> scorecardModelList = scorecardModelRepository.findScorecardModelsByClientId(loggedUser.getClientId());
        if(scorecardModelList.size() == 1){
            return 0;
        } else if (scorecardModelList.size() == 2) {
            scorecardModelRepository.delete(model);
            List<ScorecardModel> scorecardModels = scorecardModelRepository.findScorecardModelsByClientId(loggedUser.getClientId());
            for(ScorecardModel scorecardModel: scorecardModels){
                scorecardModel.setStatus(PMConstants.STATUS_ACTIVE);
                saveScorecardModel(scorecardModel);
            }
            return 1;
        }else {
            scorecardModelRepository.delete(model);
            return 1;
        }

    }

}
