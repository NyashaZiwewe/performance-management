package hr.performancemanagement.service;

import hr.performancemanagement.entities.Gear;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.GearRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GearService {
    private static final Logger log = LoggerFactory.getLogger(GearService.class);

    @Autowired
    GearRepository gearRepository;

    public Gear getGearById(long id){

        Gear gear = gearRepository.findGearById(id);
        return gear;
    }

    public List<Gear> listAllGears(long clientId)
    {
        List<Gear> gears = new ArrayList<>();
        gearRepository.findGearsByClientId(clientId).forEach(gear ->  gears.add(gear));
        return gears;
    }

    public List<Gear> listApplicableGears(long clientId, String category)
    {
        List<Gear> gears = new ArrayList<>();
        gearRepository.findGearsByClientIdAndCategory(clientId, category).forEach(gear ->  gears.add(gear));
        return gears;
    }

    public List<Gear> listGearsByReportingPeriod(long clientId, ReportingPeriod reportingPeriod)
    {
        List<Gear> gears = new ArrayList<>();
        if (reportingPeriod == null) {
            return gears;
        }
        gearRepository.findGearsByClientIdAndReportingPeriod(clientId, reportingPeriod).forEach(gear -> gears.add(gear));
        return gears;
    }

    public List<Gear> listApplicableGears(long clientId, String category, ReportingPeriod reportingPeriod)
    {
        List<Gear> gears = new ArrayList<>();
        if (reportingPeriod == null) {
            return listApplicableGears(clientId, category);
        }
        gearRepository.findGearsByClientIdAndCategoryAndReportingPeriodOrUnassigned(clientId, category, reportingPeriod)
                .forEach(gear -> gears.add(gear));
        return gears;
    }

    public List<Gear> listSelectedGears(Scorecard scorecard)
    {
        List<Gear> gears = new ArrayList<>();
        String model = getModelFromScorecard(scorecard);

        if("programme".equalsIgnoreCase(model)){
            gearRepository.selectedProgrammesByScorecard(scorecard, model).forEach(gear ->  gears.add(gear));
        }else{
            gearRepository.selectedGearsByScorecard(scorecard, model).forEach(gear ->  gears.add(gear));
        }
        for(Gear gear: gears){
            gear.setTotalAllocatedWeight(getGearTotalAllocatedWeight(scorecard.getId(), gear));
        }
        return gears;
    }

    public List<Gear> listRemainingGears(Scorecard scorecard)
    {
        List<Gear> gears = new ArrayList<>();
        String model = getModelFromScorecard(scorecard);
        long clientId = getClientIdFromScorecard(scorecard);
        List<Gear> allGears = listApplicableGears(clientId, model, scorecard == null ? null : scorecard.getReportingPeriod());
        List<Gear> remainingGears = new ArrayList<>();

        if("programme".equalsIgnoreCase(model)){
            gearRepository.selectedProgrammesByScorecard(scorecard, model).forEach(gear ->  gears.add(gear));
        }else{
            gearRepository.selectedGearsByScorecard(scorecard, model).forEach(gear ->  gears.add(gear));
        }

        for(Gear gear: allGears){
            try {
                if(!gears.contains(gear)){
                    remainingGears.add(gear);
                }
            }catch (Exception e){
                log.error("Error processing gear id={}: {}", gear.getId(), e.getMessage(), e);
            }
        }
        return remainingGears;
    }

    public double getGearTotalAllocatedWeight(long scorecardId, Gear gear){
        try {
            double total = gearRepository.sumGearAllocatedWeight(scorecardId, gear);
            return total;
        }catch (Exception e){
            return 0.0;
        }
    }

    public void addGear(Gear gear) {

        gearRepository.save(gear);
    }

    public void deleteGear(long id) {
        gearRepository.deleteById(id);
    }

    private String getModelFromScorecard(Scorecard scorecard) {
        if(scorecard.getReportingPeriod() != null && scorecard.getReportingPeriod().getModel() != null) {
            return scorecard.getReportingPeriod().getModel();
        }
        return "standard"; // default
    }

    private long getClientIdFromScorecard(Scorecard scorecard) {
        if (scorecard != null && scorecard.getClientId() > 0) {
            return scorecard.getClientId();
        }
        if (scorecard != null && scorecard.getOwner() != null && scorecard.getOwner().getClientId() > 0) {
            return scorecard.getOwner().getClientId();
        }
        return 1L;
    }

}
