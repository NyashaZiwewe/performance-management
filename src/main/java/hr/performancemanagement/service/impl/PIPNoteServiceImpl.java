package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.PIPNote;
import hr.performancemanagement.repository.PIPNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;


@Service
public class PIPNoteServiceImpl implements hr.performancemanagement.service.api.PIPNoteService {
    @Autowired
    PIPNoteRepository pipNoteRepository;

    @Override
    public List<PIPNote> listAllPIPNotes(long planId){
        List<PIPNote> pipNoteList = new ArrayList<>();
        pipNoteRepository.findPIPNotesByPerformanceImprovementPlan_id(planId).forEach(pipNote -> pipNoteList.add(pipNote));
        return pipNoteList;
    }


    @Override
    public void savePIPNote(PIPNote pipNote) {
        pipNoteRepository.save(pipNote);
    }
}
