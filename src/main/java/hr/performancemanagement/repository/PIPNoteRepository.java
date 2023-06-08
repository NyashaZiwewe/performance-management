package hr.performancemanagement.repository;

import hr.performancemanagement.entities.PIPNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PIPNoteRepository extends JpaRepository<PIPNote, Long> {
    List<PIPNote> findPIPNotesByPerformanceImprovementPlan_id(long id);

}
