package hr.performancemanagement.service;

import hr.performancemanagement.entities.Gear;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.GearRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        if (gears.isEmpty()) {
            gearRepository.findAll().forEach(gears::add);
        }
        return gears;
    }

    public List<Gear> listApplicableGears(long clientId, String category)
    {
        List<Gear> gears = new ArrayList<>();
        gearRepository.findGearsByClientIdAndCategory(clientId, category).forEach(gear ->  gears.add(gear));
        if (gears.isEmpty()) {
            gearRepository.findGearsByCategory(category).forEach(gears::add);
        }
        return gears;
    }

    public List<Gear> listGearsByReportingPeriod(long clientId, ReportingPeriod reportingPeriod)
    {
        Map<Long, Gear> byId = new LinkedHashMap<>();
        if (reportingPeriod == null || reportingPeriod.getId() <= 0) {
            return new ArrayList<>(byId.values());
        }

        long reportingPeriodId = reportingPeriod.getId();
        if (clientId > 0) {
            for (Gear gear : gearRepository.findGearsByClientIdAndReportingPeriod_Id(clientId, reportingPeriodId)) {
                if (gear != null && gear.getId() > 0) {
                    byId.put(gear.getId(), gear);
                }
            }
        } else {
            for (Gear gear : gearRepository.findGearsByReportingPeriod_Id(reportingPeriodId)) {
                if (gear != null && gear.getId() > 0) {
                    byId.put(gear.getId(), gear);
                }
            }
        }

        // Legacy fallback: support rows with nullable client_id or without a reporting_period_id.
        if (byId.isEmpty()) {
            for (Gear gear : gearRepository.findGearsByReportingPeriod_Id(reportingPeriodId)) {
                if (gear == null || gear.getId() <= 0) {
                    continue;
                }
                if (belongsToClientOrUnassigned(gear, clientId)) {
                    byId.put(gear.getId(), gear);
                }
            }
        }

        if (byId.isEmpty()) {
            LocalDate periodStart = parseDate(reportingPeriod.getStartDate());
            LocalDate periodEnd = parseDate(reportingPeriod.getEndDate());
            for (Gear gear : gearRepository.findGearsByClientId(clientId)) {
                if (gear == null || gear.getId() <= 0) {
                    continue;
                }
                if (gear.getReportingPeriod() != null) {
                    continue;
                }
                if (periodStart == null || periodEnd == null || gear.getDate() == null) {
                    continue;
                }
                LocalDate gearDate = gear.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                if ((gearDate.isEqual(periodStart) || gearDate.isAfter(periodStart))
                        && (gearDate.isEqual(periodEnd) || gearDate.isBefore(periodEnd))) {
                    byId.put(gear.getId(), gear);
                }
            }
        }
        return new ArrayList<>(byId.values());
    }

    public List<Gear> listApplicableGears(long clientId, String category, ReportingPeriod reportingPeriod)
    {
        List<Gear> gears = new ArrayList<>();
        if (reportingPeriod == null) {
            return listApplicableGears(clientId, category);
        }
        gearRepository.findGearsByClientIdAndCategoryAndReportingPeriodOrUnassigned(clientId, category, reportingPeriod)
                .forEach(gear -> gears.add(gear));
        if (gears.isEmpty()) {
            gearRepository.findGearsByCategoryAndReportingPeriodOrUnassigned(category, reportingPeriod)
                    .forEach(gears::add);
        }
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
            Double total = gearRepository.sumGearAllocatedWeight(scorecardId, gear);
            return total == null ? 0.0 : total;
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

    private boolean belongsToClientOrUnassigned(Gear gear, long clientId) {
        if (gear == null) {
            return false;
        }
        if (clientId <= 0) {
            return true;
        }
        return gear.getClientId() == clientId || gear.getClientId() <= 0;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

}
