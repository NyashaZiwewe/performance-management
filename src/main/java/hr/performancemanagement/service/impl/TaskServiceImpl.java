package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class TaskServiceImpl implements hr.performancemanagement.service.api.TaskService {
    @Autowired
    TaskRepository taskRepository;

    @Override
    public Task getTaskById(long id){

        Task task = taskRepository.findTaskById(id);
        return task;
    }

    @Override
    public List<Task> listAllTasks(long planId){
        List<Task> taskList = new ArrayList<>();
        taskRepository.findTasksByActionPlan_Id(planId).forEach(task -> taskList.add(task));
        return taskList;
    }


    @Override
    public void saveTask(Task task) {
            taskRepository.save(task);
    }

    @Transactional
    @Override
    public void deleteTask(Task task){
        taskRepository.delete(task);
    }
}
