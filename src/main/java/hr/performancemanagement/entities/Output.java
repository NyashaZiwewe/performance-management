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
    private double weight;
    @CreationTimestamp
    @Column(updatable = false)
    private Date date;
}
