package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Perspective;
import java.util.List;

public interface PerspectiveService {
    Perspective getPerspectiveById(long id);
    List<Perspective> listAllPerspectives(long clientId);
    void addPerspective(Perspective perspective);
    Perspective savePerspective(Perspective perspective);
    void deletePerspective(Perspective perspective);
}
