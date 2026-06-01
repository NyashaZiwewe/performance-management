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
    @JoinColumn(name = "output_id")
    private Output output;

    @Transient
    @ManyToOne
    @JoinColumn(name = "outcome_id")
    private Outcome outcome;

    @ManyToOne
    @JoinColumn(name = "goal_id")
    @NotNull(message = "Goal is required")
    private Goal goal;

    @Transient
    @ManyToOne
    @JoinColumn(name = "gear_id")
    private Gear gear;

    @Transient
    @ManyToOne
    @JoinColumn(name = "perspective_id")
    private Perspective perspective;

    @Transient
    @ManyToOne
    @JoinColumn(name = "strategic_objective_id")
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
    private Double actual;

    @DecimalMin(value = "0.0", message = "Employee score must be non-negative")
    @DecimalMax(value = "5.0", message = "Employee score cannot exceed 5.0")
    private Double employeeScore;

    @DecimalMin(value = "0.0", message = "Manager score must be non-negative")
    @DecimalMax(value = "5.0", message = "Manager score cannot exceed 5.0")
    private Double managerScore;

    @DecimalMin(value = "0.0", message = "Agreed score must be non-negative")
    @DecimalMax(value = "5.0", message = "Agreed score cannot exceed 5.0")
    private Double agreedScore;

    @DecimalMin(value = "0.0", message = "Moderated score must be non-negative")
    @DecimalMax(value = "5.0", message = "Moderated score cannot exceed 5.0")
    private Double moderatedScore;

    @DecimalMin(value = "0.0", message = "Weighted score must be non-negative")
    @DecimalMax(value = "100.0", message = "Weighted score cannot exceed 100")
    private Double weightedScore;

    private Double currentActual;

    @DecimalMin(value = "0.0", message = "Current employee score must be non-negative")
    @DecimalMax(value = "5.0", message = "Current employee score cannot exceed 5.0")
    private Double currentEmployeeScore;

    @DecimalMin(value = "0.0", message = "Current manager score must be non-negative")
    @DecimalMax(value = "5.0", message = "Current manager score cannot exceed 5.0")
    private Double currentManagerScore;

    @DecimalMin(value = "0.0", message = "Current agreed score must be non-negative")
    @DecimalMax(value = "5.0", message = "Current agreed score cannot exceed 5.0")
    private Double currentAgreedScore;

    @DecimalMin(value = "0.0", message = "Current moderated score must be non-negative")
    @DecimalMax(value = "5.0", message = "Current moderated score cannot exceed 5.0")
    private Double currentModeratedScore;

    @DecimalMin(value = "0.0", message = "Current weighted score must be non-negative")
    @DecimalMax(value = "100.0", message = "Current weighted score cannot exceed 100")
    private Double currentWeightedScore;
    private String currentEvidence;
    private String currentAttachmentName;
    private String currentJustification;

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
