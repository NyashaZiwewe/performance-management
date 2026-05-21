package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Note;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.repository.NoteRepository;
import hr.performancemanagement.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class NoteServiceImpl implements hr.performancemanagement.service.api.NoteService {
    @Autowired
    NoteRepository noteRepository;

    @Override
    public List<Note> listAllNotes(long planId){
        List<Note> noteList = new ArrayList<>();
        noteRepository.findNotesByActionPlan_Id(planId).forEach(note -> noteList.add(note));
        return noteList;
    }


    @Override
    public void saveNote(Note note) {
            noteRepository.save(note);
    }

    @Transactional
    @Override
    public void deleteNote(Note note){
        noteRepository.delete(note);
    }
}
