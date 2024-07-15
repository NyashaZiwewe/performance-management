package hr.performancemanagement.service;

import hr.performancemanagement.entities.OverallScore;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.OverallScoreRepository;
import hr.performancemanagement.repository.ReportingDateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OverallScoreService {
    @Autowired
    OverallScoreRepository repository;
    @Autowired
    ReportingDateRepository dateRepository;

    public OverallScore getOverallScoreByScorecardAndReportingDate(Scorecard scorecard, ReportingDate reportingDate){
        OverallScore overallScore;
        if(reportingDate == null){
            reportingDate = dateRepository.findLastReportingDateByScorecard(scorecard.getReportingPeriod());
        }
        overallScore = repository.findOverallScoreByScorecardAndReportingDate(scorecard, reportingDate);
        return overallScore;
    }

    public OverallScore saveOverallScore(OverallScore score){
        return repository.save(score);
    }
}
