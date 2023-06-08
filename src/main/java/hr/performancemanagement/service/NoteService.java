package hr.performancemanagement.service;

import hr.performancemanagement.entities.Note;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.repository.NoteRepository;
import hr.performancemanagement.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class NoteService {
    @Autowired
    NoteRepository noteRepository;

    public List<Note> listAllNotes(long planId){
        List<Note> noteList = new ArrayList<>();
        noteRepository.findNotesByActionPlan_Id(planId).forEach(note -> noteList.add(note));
        return noteList;
    }


    public void saveNote(Note note) {
            noteRepository.save(note);
    }

    @Transactional
    public void deleteNote(Note note){
        noteRepository.delete(note);
    }
}
