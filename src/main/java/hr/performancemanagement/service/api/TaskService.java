package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.repository.TaskRepository;
import java.util.ArrayList;
import java.util.List;

public interface TaskService {
    Task getTaskById(long id);
    List<Task> listAllTasks(long planId);
    void saveTask(Task task);
    void deleteTask(Task task);
}
