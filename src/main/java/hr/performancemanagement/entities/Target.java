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
    @JoinColumn(name = "output_id")
    private Output output;
    @Transient
    @ManyToOne
    @JoinColumn(name = "outcome_id")
    private Outcome outcome;
    @Transient
    @ManyToOne
    @JoinColumn(name = "goal_id")
    private Goal goal;
    @Transient
    @ManyToOne
    @JoinColumn(name = "gear_id")
    private Gear gear;
    private String measure;
    private String unit;
    private Double normalTarget;
    private Double baseTarget;
    private Double stretchTarget;
    private Double actual;
    private String currentEvidence;
    private String currentAttachmentName;
    private String currentJustification;

    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Comment> comments;
    private String flag;
    @CreationTimestamp
    private Date date;

}
