package hr.performancemanagement.service;

import hr.performancemanagement.entities.Pillar;
import hr.performancemanagement.repository.PillarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Service
public class PillarService {

    @Autowired
    PillarRepository pillarRepository;

    public Pillar findById(long id) {
        return pillarRepository.findById(id).orElse(null);
    }

    public Pillar savePillar(Pillar pillar) {
        return pillarRepository.save(pillar);
    }
}
