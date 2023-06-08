package hr.performancemanagement.service;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {
    @Autowired
    TaskRepository taskRepository;

    public Task getTaskById(long id){

        Task task = taskRepository.findTaskById(id);
        return task;
    }

    public List<Task> listAllTasks(long planId){
        List<Task> taskList = new ArrayList<>();
        taskRepository.findTasksByActionPlan_Id(planId).forEach(task -> taskList.add(task));
        return taskList;
    }


    public void saveTask(Task task) {
            taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Task task){
        taskRepository.delete(task);
    }
}
