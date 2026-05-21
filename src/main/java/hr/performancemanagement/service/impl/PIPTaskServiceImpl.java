package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.PIPTask;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.repository.PIPTaskRepository;
import hr.performancemanagement.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class PIPTaskServiceImpl implements hr.performancemanagement.service.api.PIPTaskService {
    @Autowired
    PIPTaskRepository pipTaskRepository;

    @Override
    public PIPTask getPIPTaskById(long id){

        PIPTask pipTask = pipTaskRepository.findPIPTaskById(id);
        return pipTask;
    }

    @Override
    public List<PIPTask> listAllPIPTasks(long planId){
        List<PIPTask> pipTaskList = new ArrayList<>();
        pipTaskRepository.findPIPTasksByPerformanceImprovementPlan_Id(planId).forEach(pipTask -> pipTaskList.add(pipTask));
        return pipTaskList;
    }


    @Override
    public PIPTask savePIPTask(PIPTask pipTask) {
        PIPTask task = pipTaskRepository.save(pipTask);
        return task;
    }

    @Transactional
    @Override
    public void deletePIPTask(PIPTask pipTask){
        pipTaskRepository.delete(pipTask);
    }
}
