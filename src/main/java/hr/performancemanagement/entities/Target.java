package hr.performancemanagement.entities;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Date;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Target {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "goal_id")
    private Goal goal;
    @Transient
    @ManyToOne
    @JoinColumn(name = "perspective_id")
    private Perspective perspective;
    @Transient
    @ManyToOne
    @JoinColumn(name = "strategic_objective_id")
    private StrategicObjective strategicObjective;
    private String measure;
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
    @Transient
    private String currentEvidence;
    @Transient
    private String currentJustification;

    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Comment> comments;
    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Score> scores;
    private String flag;
    @CreationTimestamp
    private Date date;

}
