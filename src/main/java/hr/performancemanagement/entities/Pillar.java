package hr.performancemanagement.entities;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Date;
import java.util.List;
@Entity
public class Pillar implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "goal_id")
    private Goal goal;
    private String name;
    @OneToMany(mappedBy = "pillar", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Outcome> outcomes;
    @Transient
    private Double weightedScore;
    @CreationTimestamp
    private Date dateAdded;


    public Pillar() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Goal getGoal() {
        return goal;
    }

    public void setGoal(Goal goal) {
        this.goal = goal;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Outcome> getOutcomes() {
        return outcomes;
    }

    public void setOutcomes(List<Outcome> outcomes) {
        this.outcomes = outcomes;
    }

    public Double getWeightedScore() {
        return weightedScore;
    }

    public void setWeightedScore(Double weightedScore) {
        this.weightedScore = weightedScore;
    }

    public Date getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }
}
