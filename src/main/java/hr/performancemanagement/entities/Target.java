package hr.performancemanagement.entities;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.*;
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
    @JoinColumn(name = "output_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Output output;

    @Transient
    private Outcome outcome;

    @ManyToOne
    @JoinColumn(name = "goal_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Goal goal;

    @Transient
    private Gear gear;

    @Transient
    private Perspective perspective;

    @Transient
    private StrategicObjective strategicObjective;

    @NotBlank(message = "Measure is required")
    @Size(max = 255, message = "Measure cannot exceed 255 characters")
    private String measure;

    @NotBlank(message = "Unit is required")
    @Size(max = 50, message = "Unit cannot exceed 50 characters")
    private String unit;

    @NotNull(message = "Allocated weight is required")
    @DecimalMin(value = "0.0", message = "Allocated weight must be non-negative")
    @DecimalMax(value = "100.0", message = "Allocated weight cannot exceed 100")
    private Double allocatedWeight;

    @NotNull(message = "Normal target is required")
    private Double normalTarget;

    private Double baseTarget;
    private Double stretchTarget;

    @Transient
    private Double actual;

    @Transient
    private Double employeeScore;

    @Transient
    private Double managerScore;

    @Transient
    private Double agreedScore;

    @Transient
    private Double moderatedScore;

    @Transient
    private Double weightedScore;

    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Evidence> evidence;
    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Comment> comments;
    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Score> scores;
    private String flag;

    @CreationTimestamp
    private Date date;

}
