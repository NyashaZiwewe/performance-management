package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Perspective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerspectiveRepository extends JpaRepository<Perspective, Long> {

    List<Perspective> findPerspectivesByClientId(long clientId);

    Perspective findPerspectiveById(long id);
}
