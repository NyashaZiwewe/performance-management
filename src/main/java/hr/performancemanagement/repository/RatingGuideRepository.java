package hr.performancemanagement.repository;

import hr.performancemanagement.entities.RatingGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface RatingGuideRepository extends JpaRepository<RatingGuide, Long> {
    //List<RatingGuide> findAll();
}
