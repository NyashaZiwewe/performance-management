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
public class ProbationAssessmentDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "assessment_id")
    private ProbationAssessment assessment;
    @ManyToOne
    @JoinColumn(name = "dimension_template_id")
    private ProbationDimensionTemplate dimensionTemplate;
    @Column(length = 3000)
    private String strengths;
    @Column(length = 3000)
    private String areasForImprovement;
    @ManyToOne
    @JoinColumn(name = "performance_improvement_plan_id")
    private PerformanceImprovementPlan performanceImprovementPlan;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp date;
}
