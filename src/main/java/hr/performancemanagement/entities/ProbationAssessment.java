package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProbationAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Account employee;
    private String performancePeriod;
    private String startDate;
    private String endDate;
    @Column(length = 3000)
    private String generalObservations;
    @Column(length = 3000)
    private String employeeComment;
    @Column(length = 3000)
    private String supervisorComment;
    @Column(length = 255)
    private String status;
    private Integer currentStepOrder;
    private String currentStepName;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp date;
}
