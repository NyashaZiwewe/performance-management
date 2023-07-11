package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.ScorecardModel;
import hr.performancemanagement.repository.ScorecardModelRepository;
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
    HttpSession session;

    public List<ScorecardModel> getAllScorecardModels(long clientId){
        List<ScorecardModel> scorecardModelList = scorecardModelRepository.findAll();
        return scorecardModelList;
    }

    public ScorecardModel getActiveScorecardModel(){
        Account loggedUser = (Account) session.getAttribute("loggedUser");
        ScorecardModel scorecardModel = scorecardModelRepository.findScorecardModelByClientIdAndStatus(loggedUser.getClientId(), "ACTIVE");
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

        scorecardModelRepository.save(model);
    }

    @Transactional
    public void deleteScorecardModel(ScorecardModel model){
        scorecardModelRepository.delete(model);
    }

}
