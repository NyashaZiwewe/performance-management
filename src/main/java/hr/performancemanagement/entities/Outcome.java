package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Outcome implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "goal_id")
    private Goal goal;
    private String name;
    @OneToMany(mappedBy = "outcome", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Output> outputs;
    @CreationTimestamp
    @Column(updatable = false)
    private Date date;

}
