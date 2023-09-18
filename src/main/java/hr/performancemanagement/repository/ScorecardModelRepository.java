package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ScorecardModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScorecardModelRepository extends JpaRepository<ScorecardModel, Long> {
    List<ScorecardModel> findScorecardModelsByClientId(long clientId);
    ScorecardModel findScorecardModelById(long id);
    ScorecardModel findScorecardModelByClientIdAndStatus(long clientId, String status);

    int countScorecardModelsByClientIdAndName(long clientId, String name);
}
