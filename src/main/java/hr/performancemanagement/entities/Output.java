package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Output implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    @ManyToOne
    @JoinColumn(name = "scorecard_id")
    private Scorecard scorecard;
    @ManyToOne
    @JoinColumn(name = "outcome_id")
    private Outcome outcome;
    @OneToMany(mappedBy = "output", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Target> targets;
    @OneToMany(mappedBy = "output", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Score> scores;
    private String unit;
    private Double allocatedWeight;
    private Double normalTarget;
    private Double baseTarget;
    private Double stretchTarget;
    private Double actual;
    private Double employeeScore;
    private Double managerScore;
    private Double agreedScore;
    private Double moderatedScore;
    private Double weightedScore;
    private Double currentActual;
    private Double currentEmployeeScore;
    private Double currentManagerScore;
    private Double currentAgreedScore;
    private Double currentModeratedScore;
    private Double currentWeightedScore;
    @CreationTimestamp
    @Column(updatable = false)
    private Date date;
}
