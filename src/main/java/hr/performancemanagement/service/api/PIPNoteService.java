package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.PIPNote;
import java.util.List;

public interface PIPNoteService {
    List<PIPNote> listAllPIPNotes(long planId);
    void savePIPNote(PIPNote pipNote);
}
