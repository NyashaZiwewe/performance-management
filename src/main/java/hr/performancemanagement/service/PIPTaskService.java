package hr.performancemanagement.service;

import hr.performancemanagement.entities.PIPTask;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.repository.PIPTaskRepository;
import hr.performancemanagement.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PIPTaskService {
    @Autowired
    PIPTaskRepository pipTaskRepository;

    public PIPTask getPIPTaskById(long id){

        PIPTask pipTask = pipTaskRepository.findPIPTaskById(id);
        return pipTask;
    }

    public List<PIPTask> listAllPIPTasks(long planId){
        List<PIPTask> pipTaskList = new ArrayList<>();
        pipTaskRepository.findPIPTasksByPerformanceImprovementPlan_Id(planId).forEach(pipTask -> pipTaskList.add(pipTask));
        return pipTaskList;
    }


    public PIPTask savePIPTask(PIPTask pipTask) {
        PIPTask task = pipTaskRepository.save(pipTask);
        return task;
    }

    @Transactional
    public void deletePIPTask(PIPTask pipTask){
        pipTaskRepository.delete(pipTask);
    }
}
