package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PIPNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "performance_improvement_plan_id")
    private PerformanceImprovementPlan performanceImprovementPlan;
    private String comment;
    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Account employee;
    private String status;
    @CreationTimestamp
    private Date date;

}
