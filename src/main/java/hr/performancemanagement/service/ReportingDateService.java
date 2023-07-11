package hr.performancemanagement.service;

import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingDateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReportingDateService {
    @Autowired
    ReportingDateRepository reportingDateRepository;

    public ReportingDate getReportingDateById(long id){

        ReportingDate reportingDate = reportingDateRepository.findReportingDateById(id);
        return reportingDate;
    }

    public List<ReportingDate> listAllReportingDates(ReportingPeriod reportingPeriod){
        List<ReportingDate> reportingDateList = new ArrayList<>();
        reportingDateRepository.findReportingDatesByReportingPeriod(reportingPeriod).forEach(reportingDate -> reportingDateList.add(reportingDate));
        return reportingDateList;
    }


    public void saveReportingDate(ReportingDate reportingDate) {
        reportingDateRepository.save(reportingDate);
    }

    @Transactional
    public void deleteReportingDate(ReportingDate reportingDate){
        reportingDateRepository.delete(reportingDate);
    }
}
