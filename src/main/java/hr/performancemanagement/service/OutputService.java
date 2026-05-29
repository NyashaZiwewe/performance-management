package hr.performancemanagement.service;

import hr.performancemanagement.entities.Output;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.OutputRepository;
import org.hibernate.result.Outputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutputService {
    private static final Logger log = LoggerFactory.getLogger(OutputService.class);

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
           log.error("Error saving output: {}", output != null ? output.getName() : "null", e);
           throw new RuntimeException("Failed to save output", e);
       }
    }

    public boolean outputExists(String name){
        return outputRepository.existsByName(name);
    }

    public boolean outputExistsOnScorecard(String name, Scorecard scorecard){
        return outputRepository.existsByNameAndScorecard(name, scorecard);
    }

    public List<Output> listAllOutputs(Scorecard scorecard){
        return outputRepository.findOutputsByScorecard(scorecard);
    }

    public List<Output> listAllOutputs(){
        return outputRepository.findAll();
    }

    public void deleteOutput(Output output){
        outputRepository.delete(output);
    }
}
