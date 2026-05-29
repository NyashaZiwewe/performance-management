package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.sql.Date;
import java.util.List;


@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Goal implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "gear_id")
    private Gear gear;

    @Positive(message = "Scorecard ID must be positive")
    private long scorecardId;

    @ManyToOne
    @JoinColumn(name = "perspective_id")
    private Perspective perspective;

    @ManyToOne
    @JoinColumn(name = "strategic_objective_id")
    private StrategicObjective strategicObjective;

    @NotBlank(message = "Goal name is required")
    @Size(min = 3, max = 500, message = "Goal name must be between 3 and 500 characters")
    private String name;

    @OneToMany(mappedBy = "goal", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Outcome> outcomes;

    @OneToMany(mappedBy = "goal", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Pillar> pillars;

    @Transient
    private Double weightedScore;

    @CreationTimestamp
    private Date dateAdded;

}
