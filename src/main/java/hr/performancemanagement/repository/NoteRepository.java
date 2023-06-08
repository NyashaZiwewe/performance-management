package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findNotesByActionPlan_Id(long id);

}
