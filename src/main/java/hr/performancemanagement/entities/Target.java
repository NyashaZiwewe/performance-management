package hr.performancemanagement.entities;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.sql.Date;

@Entity
public class Target {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long goalId;
    private String measure;
    private String unit;
    private double allocatedWeight;
    private double target;
    private double employeeScore;
    private double managerScore;
    private double actualScore;
    private String supportingDocument;
    private String justification;
    private String comment;
    private Date date;

    public Target(long id, long goalId, String measure, String unit, double allocatedWeight, double target, double employeeScore, double managerScore, double actualScore, String supportingDocument, String justification, String comment, Date date) {
        this.id = id;
        this.goalId = goalId;
        this.measure = measure;
        this.unit = unit;
        this.allocatedWeight = allocatedWeight;
        this.target = target;
        this.employeeScore = employeeScore;
        this.managerScore = managerScore;
        this.actualScore = actualScore;
        this.supportingDocument = supportingDocument;
        this.justification = justification;
        this.comment = comment;
        this.date = date;
    }

    public Target() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getGoalId() {
        return goalId;
    }

    public void setGoalId(long goalId) {
        this.goalId = goalId;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
