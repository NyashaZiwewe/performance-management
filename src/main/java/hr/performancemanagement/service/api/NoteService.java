package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Note;
import java.util.List;

public interface NoteService {
    List<Note> listAllNotes(long planId);
    void saveNote(Note note);
}
