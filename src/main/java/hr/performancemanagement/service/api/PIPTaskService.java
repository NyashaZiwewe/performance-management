package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.PIPTask;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.repository.PIPTaskRepository;
import hr.performancemanagement.repository.TaskRepository;
import java.util.ArrayList;
import java.util.List;

public interface PIPTaskService {
    PIPTask getPIPTaskById(long id);
    List<PIPTask> listAllPIPTasks(long planId);
    PIPTask savePIPTask(PIPTask pipTask);
    void deletePIPTask(PIPTask pipTask);
}
