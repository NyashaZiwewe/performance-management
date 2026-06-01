package hr.performancemanagement.entities;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Gear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    private String name;
    private String category;
    private String description;
    private String graphColor;
    private String fill;
    @ManyToOne
    @JoinColumn(name = "reporting_period_id")
    private ReportingPeriod reportingPeriod;
    @OneToMany(mappedBy = "gear", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Goal> goals;
    @Transient
    private double totalAllocatedWeight;
    @Transient
    private List<Target> targetsList;
    @Transient
    private List<Outcome> outcomes;
    @CreationTimestamp
    @Column(updatable = false)
    private Date date;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Gear gear = (Gear) o;
        return id == gear.id;
    }
}
