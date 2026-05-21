package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Note;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.repository.NoteRepository;
import hr.performancemanagement.repository.TaskRepository;
import java.util.ArrayList;
import java.util.List;

public interface NoteService {
    List<Note> listAllNotes(long planId);
    void saveNote(Note note);
    void deleteNote(Note note);
}
