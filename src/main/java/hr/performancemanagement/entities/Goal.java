package hr.performancemanagement.entities;

import org.springframework.data.jpa.repository.Query;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

@Entity
public class Goal implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long scorecardId;
    @ManyToOne
    @JoinColumn(name = "perspective_id")
    private Perspective perspective;
    @ManyToOne
    @JoinColumn(name = "strategic_objective_id")
    private StrategicObjective strategicObjective;
    private String name;
    private String measure;
    private String unit;
    private double allocatedWeight;
    private double target;
    @Column(columnDefinition = "double default null")
    private double employeeScore;
    @Column(columnDefinition = "double default null")
    private double managerScore;
    @Column(columnDefinition = "double default null")
    private double actualScore;
    private String supportingDocument;
    private String justification;

    @OneToMany(mappedBy = "goal", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Comment> comments;
    private String flag;

    public Goal() {
    }

    public Goal(long id, long scorecardId, Perspective perspective, StrategicObjective strategicObjective, String name, String measure, String unit, double allocatedWeight, double target, double employeeScore, double managerScore, double actualScore, String supportingDocument, String justification, List<Comment> comments, String flag) {
        this.id = id;
        this.scorecardId = scorecardId;
        this.perspective = perspective;
        this.strategicObjective = strategicObjective;
        this.name = name;
        this.measure = measure;
        this.unit = unit;
        this.allocatedWeight = allocatedWeight;
        this.target = target;
        this.employeeScore = employeeScore;
        this.managerScore = managerScore;
        this.actualScore = actualScore;
        this.supportingDocument = supportingDocument;
        this.justification = justification;
        this.comments = comments;
        this.flag = flag;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getScorecardId() {
        return scorecardId;
    }

    public void setScorecardId(long scorecardId) {
        this.scorecardId = scorecardId;
    }

    public Perspective getPerspective() {
        return perspective;
    }

    public void setPerspective(Perspective perspective) {
        this.perspective = perspective;
    }

    public StrategicObjective getStrategicObjective() {
        return strategicObjective;
    }

    public void setStrategicObjective(StrategicObjective strategicObjective) {
        this.strategicObjective = strategicObjective;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getAllocatedWeight() {
        return allocatedWeight;
    }

    public void setAllocatedWeight(double allocatedWeight) {
        this.allocatedWeight = allocatedWeight;
    }

    public double getTarget() {
        return target;
    }

    public void setTarget(double target) {
        this.target = target;
    }

    public double getEmployeeScore() {
        return employeeScore;
    }

    public void setEmployeeScore(double employeeScore) {
        this.employeeScore = employeeScore;
    }

    public double getManagerScore() {
        return managerScore;
    }

    public void setManagerScore(double managerScore) {
        this.managerScore = managerScore;
    }

    public double getActualScore() {
        return actualScore;
    }

    public void setActualScore(double actualScore) {
        this.actualScore = actualScore;
    }

    public String getSupportingDocument() {
        return supportingDocument;
    }

    public void setSupportingDocument(String supportingDocument) {
        this.supportingDocument = supportingDocument;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

//    @Query("SELECT '*' FROM Comment WHERE goal = :goal ORDER BY id")
    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }


}
