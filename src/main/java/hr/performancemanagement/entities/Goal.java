package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
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
    private String name;
    @OneToMany(mappedBy = "goal", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Outcome> outcomes;
//    @ManyToOne
//    @JoinColumn(name = "reporting_period_id")
//    private ReportingPeriod reportingPeriod;
    @Transient
    private Double weightedScore;
    @CreationTimestamp
    private Date dateAdded;

}
