package hr.performancemanagement.service;

import hr.performancemanagement.entities.Output;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.OutputRepository;
import org.hibernate.result.Outputs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutputService {
    @Autowired
    OutputRepository outputRepository;

    public Output getOutputById (long id){
        return outputRepository.findOutputById(id);
    }

    public Output getOutputByName (String name){
        return outputRepository.findOutputByName(name);
    }
    public Output saveOutput(Output output){
       try {
           return outputRepository.save(output);
       }catch (Exception e){
           System.out.println(e.getMessage());
           return null;
       }
    }

    public boolean outputExists(String name){
        return outputRepository.existsByName(name);
    }

    public List<Output> listAllOutputs(Scorecard scorecard){
        return outputRepository.findOutputsByScorecard(scorecard);
    }

    public void deleteOutput(Output output){
        outputRepository.delete(output);
    }
}
