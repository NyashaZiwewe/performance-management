package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Note;
import hr.performancemanagement.entities.PIPNote;
import hr.performancemanagement.repository.NoteRepository;
import hr.performancemanagement.repository.PIPNoteRepository;
import java.util.ArrayList;
import java.util.List;

public interface PIPNoteService {
    List<PIPNote> listAllPIPNotes(long planId);
    void savePIPNote(PIPNote pipNote);
    void deletePIPNote(PIPNote pipNote);
}
