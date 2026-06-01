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
public class ProbationKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "assessment_id")
    private ProbationAssessment assessment;
    private String name;
    @Column(length = 1000)
    private String measureOfSuccess;
    @Column(length = 1000)
    private String target;
    private Double incumbentMark;
    private Double supervisorMark;
    private Double progressPercent;
    @Column(length = 3000)
    private String progressComment;
    @Column(length = 3000)
    private String incumbentComment;
    @Column(length = 3000)
    private String supervisorComment;
    @Column(length = 1000)
    private String attachmentPath;
    @Column(length = 2000)
    private String flag;
    private String status;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp date;
}
