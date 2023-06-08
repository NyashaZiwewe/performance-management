package hr.performancemanagement.entities;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.sql.Date;

@Entity
public class Response360 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private int employeeId;
    private int assessorId;
    private int questionId;
    private int value;
    private int comment;
    @CreatedDate
    private Date date;

    public Response360(long id, int employeeId, int assessorId, int questionId, int value, int comment, Date date) {
        this.id = id;
        this.employeeId = employeeId;
        this.assessorId = assessorId;
        this.questionId = questionId;
        this.value = value;
        this.comment = comment;
        this.date = date;
    }

    public Response360() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employee_id) {
        this.employeeId = employeeId;
    }

    public int getAssessorId() {
        return assessorId;
    }

    public void setAssessorId(int assessor_id) {
        this.assessorId = assessorId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getComment() {
        return comment;
    }

    public void setComment(int comment) {
        this.comment = comment;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
