package hr.performancemanagement.service;

import hr.performancemanagement.entities.Gear;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.GearRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GearService {

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

    public List<Gear> listSelectedGears(Scorecard scorecard)
    {
        List<Gear> gears = new ArrayList<>();
        gearRepository.selectedGearsByScorecard(scorecard).forEach(gear ->  gears.add(gear));
        for(Gear gear: gears){
            gear.setTotalAllocatedWeight(getGearTotalAllocatedWeight(scorecard.getId(), gear));
        }
        return gears;
    }

    public List<Gear> listRemainingGears(Scorecard scorecard)
    {
        List<Gear> gears = new ArrayList<>();
        List<Gear> allGears = gearRepository.findAll();
        List<Gear> remainingGears = new ArrayList<>();
        gearRepository.selectedGearsByScorecard(scorecard).forEach(gear ->  gears.add(gear));

        for(Gear gear: allGears){
            try {
                if(!gears.contains(gear)){
                    remainingGears.add(gear);
                }
            }catch (Exception e){
                System.out.println(e.getMessage());
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

}
