package hr.performancemanagement.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.sql.Date;

@Entity
public class Question360 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private int stepId;
    private int dimension;
    private String period;
    private String question;
    private Date date;

    public Question360(long id, int stepId, int dimension, String period, String question, Date date) {
        this.id = id;
        this.stepId = stepId;
        this.dimension = dimension;
        this.period = period;
        this.question = question;
        this.date = date;
    }

    public Question360() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getStepId() {
        return stepId;
    }

    public void setStepId(int step_id) {
        this.stepId = stepId;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }
    private String getPeriod(){ return period; }
    private void setPeriod(String period){this.period = period; }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
