package hr.performancemanagement.repository;

import hr.performancemanagement.entities.BusinessUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface BusinessUnitRepository extends JpaRepository<BusinessUnit, Long> {

    List<BusinessUnit> findBusinessUnitsByClientId(long clientId);
}
